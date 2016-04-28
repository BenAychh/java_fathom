package io.benaychh.java_fathom;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Based on https://github.com/ogrodnek/java_fathom.
 */

/**
 * <p>
 * Readability and general measurements of English text.
 * </p>
 * 
 * <p>
 * Ported from perl's Lingua::EN::Fathom by Kim Ryan.
 * </p>
 * 
 * <p>
 * <b>COPYRIGHT</b><br>
 * Distributed under the same terms as Perl.
 * </p>
 * 
 * @author Kim Ryan
 * @author Larry Ogrodnek &lt;ogrodnek@gmail.com&gt;
 * 
 * @version $Revision: 1.2 $ $Date: 2007-10-22 01:28:51 $
 */
public final class Kincaid {
  private static final String[] abbreviations = new String[] {
  // personal titles
      "Mr", "Mrs", "M", "Dr", "Prof", "Det", "Insp",
      // Commercial abbreviations
      "Pty", "PLC", "Ltd", "Inc",
      // Other abbreviations
      "etc", "vs", };

  
  public static float analyze(final String s) {
    Stats stats = analyzeWords(new Stats(), s);
    float score = (11.8f * syllablesPerWords(stats))
            + (0.39f * wordsPerSentence(stats)) - 15.59f;
    if (score >= 1) {
      return score;
    }
    return 0;
  }
  
  public static float syllablesPerWords(final Stats stats) {
    return ((float) stats.getNumSyllables()) / stats.getNumWords();
  }
  
  public static float wordsPerSentence(final Stats stats) {
    return ((float) stats.getNumWords()) / stats.getNumSentences();
  }

  // Word found, such as: twice, BOTH, a, I'd, non-plussed ..
  // Ignore words like K12, &, X.Y.Z ...

  private static final Pattern WORD_PAT
      = Pattern.compile("\\b([a-z][-'a-z]*)\\b");

  private static final Pattern VOWELS = Pattern.compile("[aeiouy]");

  private static final Pattern VALID_HYPHENS = Pattern.compile("[a-z]{2,}-[a-z]{2,}");

  private static final Pattern END_SENTENCE = Pattern.compile("\\b\\s*[.!?]\\s*\\b");

  private static final Pattern END_SENTENCE_END_LINE = Pattern.compile("\\b\\s*[.!?]\\s*$");

  private static Stats analyzeWords(final Stats _stats, final String _s) {
    final Stats stats = _stats == null ? new Stats() : _stats;

    String s = _s.toLowerCase().trim();

    Matcher m = WORD_PAT.matcher(s);

    while (m.find()) {
      final String word = m.group(1);

      // Try to filter out acronyms and abbreviations by accepting
      // words with a vowel sound. This won't work for GPO etc.
      if (!VOWELS.matcher(word).find()) {
        continue;
      }

      // Test for valid hyphenated word like be-bop
      if (word.indexOf('-') > 0 && (!VALID_HYPHENS.matcher(word).matches())) {
        continue;
      }

      // word frequency count
      stats.addWord(word);

      final int syl = Syllable.syllable(word);

      stats.numSyllables += syl;

      // Required for Fog index, count non hyphenated words of 3 or more
      // syllables. Should add check for proper names in here as well
      if (syl > 2 && word.indexOf('-') < 0) {
        stats.numComplexWords++;
      }
    }

    // replace common abbreviations to ease the search for end of sentence.
    s = replaceAbbr(s);

    // clean out quotes for same reason
    s.replaceAll("[\"']", "");

    // Search for '.', '?' or '!' to end a sentence.
    m = END_SENTENCE.matcher(s);

    while (m.find()) {
      stats.numSentences++;
    }

    // Check for final sentence, with no following words.
    m = END_SENTENCE_END_LINE.matcher(s);

    if (m.find()) {
      stats.numSentences++;
    }

    return stats;
  }

  private static final String replaceAbbr(final String s) {
    String ret = s;

    for (final String a : abbreviations) {
      ret = ret.replaceAll("\\s" + a + "\\.\\s", a);
    }

    return ret;
  }

  public static final class Stats {
    private int numWords = 0;
    private int numSentences = 0;
    private final int numTextLines = 0;
    private final int numBlankLines = 0;
    private int numSyllables = 0;
    private int numComplexWords = 0;

    private final Map<String, Integer> uniqueWords = new HashMap<String, Integer>();

    private Stats() { }

    @Override
    public String toString() {
      return String.format("Stats:[words: %d, sentences: %d, text: %d, blank: %d, syllables: %d, complex: %d]",
          this.numWords, this.numSentences, this.numTextLines, this.numBlankLines, this.numSyllables,
          this.numComplexWords);
    }

    private void addWord(final String s) {
      final Integer i = this.uniqueWords.get(s);
      this.uniqueWords.put(s, i == null ? 1 : 1 + i.intValue());
      this.numWords++;
    }

    public int getNumBlankLines() {
      return this.numBlankLines;
    }

    public int getNumSentences() {
      if (this.numSentences != 0) {
        return this.numSentences;
      }
      return 1;
    }

    public int getNumTextLines() {
      return this.numTextLines;
    }

    public int getNumWords() {
      return this.numWords;
    }

    public int getNumComplexWords() {
      return this.numComplexWords;
    }

    public int getNumSyllables() {
      return this.numSyllables;
    }

    public Map<String, Integer> getUniqueWords() {
      return Collections.unmodifiableMap(this.uniqueWords);
    }
  }
}