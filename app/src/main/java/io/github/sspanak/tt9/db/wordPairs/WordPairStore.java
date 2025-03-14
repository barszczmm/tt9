package io.github.sspanak.tt9.db.wordPairs;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.sspanak.tt9.db.BaseSyncStore;
import io.github.sspanak.tt9.db.sqlite.DeleteOps;
import io.github.sspanak.tt9.db.sqlite.InsertOps;
import io.github.sspanak.tt9.db.sqlite.ReadOps;
import io.github.sspanak.tt9.db.words.DictionaryLoader;
import io.github.sspanak.tt9.languages.Language;
import io.github.sspanak.tt9.preferences.settings.SettingsStore;
import io.github.sspanak.tt9.util.Logger;
import io.github.sspanak.tt9.util.Timer;

public class WordPairStore extends BaseSyncStore {
	private static final String LOG_TAG = WordPairStore.class.getSimpleName();

	// data
	private final ConcurrentHashMap<Integer, HashMap<WordPair, WordPair>> pairs = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Integer, HashMap<String, ArrayList<String>>> suggestions = new ConcurrentHashMap<>();

	// timing
	private long slowestAddTime = 0;
	private long slowestLoadTime = 0;
	private long slowestSaveTime = 0;
	private long slowestSearchTime = 0;


	public WordPairStore(Context context) {
		super(context);
	}


	private void addNextWordSuggestion(HashMap<String, ArrayList<String>> languageSuggestions, String word1, String word2) {
		ArrayList<String> nextWordSuggestions = languageSuggestions.get(word1);
		if (nextWordSuggestions == null) {
			nextWordSuggestions = new ArrayList<>();
			nextWordSuggestions.add(word2);
		} else {
			int word2Index = nextWordSuggestions.indexOf(word2);
			if (word2Index < 0) {
				nextWordSuggestions.add(0, word2);
			} else {
				// Order in the array matters: last added word will be suggested as first in suggestions list.
				nextWordSuggestions.remove(word2Index);
				nextWordSuggestions.add(0, word2);
			}
		}
	}

	private void addToNextWordSuggestions(Language language, String word1, String word2) {
		HashMap<String, ArrayList<String>> languageSuggestions = suggestions.get(language.getId());
		if (languageSuggestions == null) {
			languageSuggestions = new HashMap<>();
			suggestions.put(language.getId(), languageSuggestions);
		}
		word1 = word1.toLowerCase(language.getLocale());
		word2 = word2.toLowerCase(language.getLocale());
		addNextWordSuggestion(languageSuggestions, word1, word2);
	}


	public void add(Language language, String word1, String word2, String sequence2) {
		String ADD_TIMER_NAME = "word_pair_add";
		Timer.start(ADD_TIMER_NAME);

		WordPair pair = new WordPair(language, word1, word2, sequence2);
		if (pair.isInvalid()) {
			return;
		}

		HashMap<WordPair, WordPair> languagePairs = pairs.get(language.getId());
		if (languagePairs == null) {
			languagePairs = new HashMap<>();
			pairs.put(language.getId(), languagePairs);
		}

		if (languagePairs.size() >= SettingsStore.WORD_PAIR_MAX) {
			languagePairs.remove(languagePairs.keySet().iterator().next());
		}

		languagePairs.put(pair, pair);

		addToNextWordSuggestions(language, word1, word2);

		slowestAddTime = Math.max(slowestAddTime, Timer.stop(ADD_TIMER_NAME));
	}


	public void clearCache() {
		pairs.clear();
		suggestions.clear();
		slowestAddTime = 0;
		slowestSearchTime = 0;
		slowestSaveTime = 0;
		slowestLoadTime = 0;
	}


	@Nullable
	public String getWord2(Language language, String word1, String sequence2) {
		String SEARCH_TIMER_NAME = "word_pair_search";
		Timer.start(SEARCH_TIMER_NAME);

		HashMap<WordPair, WordPair> languagePairs = pairs.get(language.getId());

		if (languagePairs == null) {
			slowestSearchTime = Math.max(slowestSearchTime, Timer.stop(SEARCH_TIMER_NAME));
			return null;
		}

		WordPair pair = languagePairs.get(new WordPair(language, word1, null, sequence2));
		String word2 = pair == null || pair.getWord2().isEmpty() ? null : pair.getWord2();

		slowestSearchTime = Math.max(slowestSearchTime, Timer.stop(SEARCH_TIMER_NAME));
		return word2;
	}


	@Nullable
	public ArrayList<String> getNextWordSuggestions(Language language, String word1) {
		String SEARCH_TIMER_NAME = "word_pairs_search";
		Timer.start(SEARCH_TIMER_NAME);

		HashMap<String, ArrayList<String>> languageSuggestions = suggestions.get(language.getId());

		if (languageSuggestions == null) {
			slowestSearchTime = Math.max(slowestSearchTime, Timer.stop(SEARCH_TIMER_NAME));
			return null;
		}

		word1 = word1.toLowerCase(language.getLocale());
		ArrayList<String> nextWordSuggestions = languageSuggestions.get(word1);

		slowestSearchTime = Math.max(slowestSearchTime, Timer.stop(SEARCH_TIMER_NAME));
		return nextWordSuggestions;
	}


	public void save() {
		if (!checkOrNotify()) {
			return;
		}

		String SAVE_TIMER_NAME = "word_pair_save";
		Timer.start(SAVE_TIMER_NAME);

		for (Map.Entry<Integer, HashMap<WordPair, WordPair>> entry : pairs.entrySet()) {
			int langId = entry.getKey();
			HashMap<WordPair, WordPair> languagePairs = entry.getValue();

			sqlite.beginTransaction();
			DeleteOps.deleteWordPairs(sqlite.getDb(), langId);
			InsertOps.insertWordPairs(sqlite.getDb(), langId, languagePairs.values());
			sqlite.finishTransaction();
		}


		long currentTime = Timer.stop(SAVE_TIMER_NAME);
		slowestSaveTime = Math.max(slowestSaveTime, currentTime);
		Logger.d(LOG_TAG, "Saved all word pairs in: " + currentTime + " ms");
	}


	public void load(@NonNull DictionaryLoader dictionaryLoader, ArrayList<Language> languages) {
		if (!checkOrNotify()) {
			return;
		}

		if (dictionaryLoader.isRunning()) {
			Logger.e(LOG_TAG, "Cannot load word pairs while the DictionaryLoader is working.");
			return;
		}

		if (languages == null) {
			Logger.e(LOG_TAG, "Cannot load word pairs for NULL language list.");
			return;
		}

		String LOAD_TIMER_NAME = "word_pair_load";
		Timer.start(LOAD_TIMER_NAME);

		int totalPairs = 0;
		for (Language language : languages) {
			if (language.isSyllabary()) {
				Logger.d(LOG_TAG, "Not loading word pairs for syllabary language: " + language.getId());
				continue;
			}

			HashMap<WordPair, WordPair> wordPairs = pairs.get(language.getId());
			if (wordPairs == null) {
				wordPairs = new HashMap<>();
				pairs.put(language.getId(), wordPairs);
			} else if (!wordPairs.isEmpty()) {
				continue;
			}

			HashMap<String, ArrayList<String>> languageSuggestions = suggestions.get(language.getId());
			if (languageSuggestions == null) {
				languageSuggestions = new HashMap<>();
				suggestions.put(language.getId(), languageSuggestions);
			} else if (!languageSuggestions.isEmpty()) {
				languageSuggestions.clear();
			}

			int max = SettingsStore.WORD_PAIR_MAX - wordPairs.size();
			ArrayList<WordPair> dbPairs = new ReadOps().getWordPairs(sqlite.getDb(), language, max);
			for (WordPair pair : dbPairs) {
				wordPairs.put(pair, pair);
				addNextWordSuggestion(languageSuggestions, pair.getWord1(), pair.getWord2());
			}

			Logger.d(LOG_TAG, "Loaded " + wordPairs.size() + " word pairs for language: " + language.getId());
		}

		long currentTime = Timer.stop(LOAD_TIMER_NAME);
		slowestLoadTime = Math.max(slowestLoadTime, currentTime);
		Logger.d(LOG_TAG, "Loaded " + totalPairs + " word pairs in " + currentTime + " ms");
	}


	public void remove(@NonNull ArrayList<Language> languages) {
		if (!checkOrNotify()) {
			return;
		}

		Timer.start(LOG_TAG);

		for (Language language : languages) {
			DeleteOps.deleteWordPairs(sqlite.getDb(), language.getId());
		}

		Logger.d(LOG_TAG, "Deleted " + languages.size() + " word pair groups. Time: " + Timer.stop(LOG_TAG) + " ms");

		slowestLoadTime = 0;
		slowestSaveTime = 0;
	}


	@NonNull
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		for (Map.Entry<Integer, HashMap<WordPair, WordPair>> entry : pairs.entrySet()) {
			int langId = entry.getKey();
			HashMap<WordPair, WordPair> languagePairs = entry.getValue();

			sb.append("Language ").append(langId).append(": ");
			sb.append(languagePairs.size()).append("\n");
		}

		if (sb.length() == 0) {
			sb.append("No word pairs.\n");
		} else {
			sb.append("\nSlowest add-one: ").append(slowestAddTime).append(" ms\n");
			sb.append("Slowest search-one: ").append(slowestSearchTime).append(" ms\n");
			sb.append("Slowest save-all: ").append(slowestSaveTime).append(" ms\n");
			sb.append("Slowest load-all: ").append(slowestLoadTime).append(" ms\n");
		}

		return sb.toString();
	}
}
