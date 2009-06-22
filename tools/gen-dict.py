#!/usr/bin/python2.4

"""Creates a dictionary for net.cactii.target.

For now, doesnt filter out plurals, as that's too hard and
time consuming.
"""

import wordlist

import os
import random
import re
import string
import sys

from optparse import OptionParser

class Dictionaries(object):
  """
    original_files: A dict, key=language, value=filename
    originals: A dict of lists. key is language (eg 'uk'),
               inner is list of words.
    common_words: List of common words
    nineletter_words: Dict of lists, nineletter words
    common_words_by_letter: Dict of common words, indexed by letter
    unique_words: Dict (by dictionary) of word lists
    used_words: List of words used by the nine-letters
    plurals: A list of plurals
  """
  def __init__(self, options):
    self.options = options
    self.original_files = {}
    self.originals = {}
    self.common_words = None
    self.common_words_by_letter = {}
    self.plurals = None
    # for i in xrange(97, 123):
    #   self.common_words_by_letter[chr(i)] = []
    
    self.unique_words = {}
    self.nineletter_words = {}
    self.used_words = set()

  def FixLanguageDictionaries(self):
    ref_uk = wordlist.Dict('/usr/share/dict/british-english')
    ref_us = wordlist.Dict('/usr/share/dict/american-english')
    ref_uk_byletter = {}
    ref_us_byletter = {}
    for word in ref_uk:
        letter = word[0]
        if letter not in ref_uk_byletter:
            ref_uk_byletter[letter] = wordlist.Dict()
        ref_uk_byletter[letter].append(word)
    for word in ref_us:
        letter = word[0]
        if letter not in ref_us_byletter:
            ref_us_byletter[letter] = wordlist.Dict()
        ref_us_byletter[letter].append(word)

    correct_uk = []
    index = 0
    for word in self.originals['uk']:
      letter = word[0]
      if not (index % 100):
        print '\r%-6d' % index,
        sys.stdout.flush()
      index += 1
      if word in ref_us_byletter[letter] and word not in ref_uk_byletter[letter]:
        continue
      correct_uk.append(word)
    self.originals['uk'].replace(correct_uk)

    correct_us = []
    index = 0
    for word in self.originals['us']:
      letter = word[0]
      if not (index % 100):
        print '\r%-6d' % index,
        sys.stdout.flush()
      index += 1
      if word in ref_uk_byletter[letter] and word not in ref_us_byletter[letter]:
        continue
      correct_us.append(word)
    self.originals['us'].replace(correct_us)
    
    self.originals['uk'].writeToFile('uk-originals.txt')
    self.originals['us'].writeToFile('us-originals.txt')
    sys.exit()

  def GenerateCommonWords(self):
    dicts = self.originals.keys()
    for d in dicts:
      self.unique_words[d] = wordlist.Dict()
    num_dicts = len(dicts)
    dicts_per_word = {}  # key = word, value = list of dicts
    # Generates the dict, would like like, eg:
    # { 'else' : ['uk, 'us'], 'colour' : ['uk']}
    for d,words in self.originals.iteritems():
      for word in words:
        if word not in dicts_per_word:
          dicts_per_word[word] = []
        dicts_per_word[word].append(d)

    self.common_words = wordlist.Dict()
    for word,dicts in dicts_per_word.iteritems():
      if len(dicts) == num_dicts:
        self.common_words.append(word)
      else:
        for d in dicts:
          self.unique_words[d].append(word)
    self.common_words.sort()

  def GenerateWordsByLetter(self):
    for i in xrange(65, 91):
      self.common_words_by_letter[chr(i)] = wordlist.Dict()
    for word in self.common_words:
      if len(word) != 9:
        self.common_words_by_letter[word[0]].append(word)
    
  def GetNineLetterWords(self):
    allowed_nineletters = wordlist.Dict()
    total_nineletters = self.common_words.getWordsByLength(9)
    for d,words in self.unique_words.iteritems():
      total_nineletters.extend(words.getWordsByLength(9))

    # total_nineletters contains all 9letter words
    num_nines = len(total_nineletters)

    for word in total_nineletters:
      if (word.endswith('s') and not word.endswith('ess') and not
          word.endswith('ous')):
        continue
      if word not in allowed_nineletters:
        allowed_nineletters.append(word)

    while False and len(allowed_nineletters) < 3000:
      random_word = total_nineletters[random.randint(0, num_nines-1)]
      if (random_word.endswith('s') and not random_word.endswith('ess') and not
          random_word.endswith('ous')):
        continue
      if random_word not in allowed_nineletters:
        allowed_nineletters.append(random_word)

    self.nineletter_words = {'common': wordlist.Dict()}
    for word in self.common_words.getWordsByLength(9):
      if word in allowed_nineletters:
        self.nineletter_words['common'].append(word)
        self.common_words.remove(word)

    for d,words in self.unique_words.iteritems():
      self.nineletter_words[d] = wordlist.Dict()
      for word in words.getWordsByLength(9):
        if word in allowed_nineletters:
          self.nineletter_words[d].append(word)
          self.unique_words[d].remove(word)

  def WriteWordFiles(self, output_dir):
    for d,words in self.unique_words.iteritems():
      words.sort()
      words.writeToFile('%s/words_%s' % (output_dir, d))

    for letter,words in self.common_words_by_letter.iteritems():
      words.sort()
      words.writeToFile('%s/words_common_%s' % (output_dir, letter.lower()))

    for d,words in self.nineletter_words.iteritems():
      words.sort()
      words.writeToFile('%s/nineletter_%s' % (output_dir, d))

  def RemoveUnusedWords(self):
    newlist = {}
    for k, words in self.unique_words.iteritems():
      newlist[k] = wordlist.Dict()
      for w in words:
        if w in self.used_words:
          newlist[k].append(w)
    self.unique_words = newlist

    newlist = {}
    for k, words in self.common_words_by_letter.iteritems():
      newlist[k] = wordlist.Dict()
      for w in words:
        if w in self.used_words:
          newlist[k].append(w)
    self.common_words_by_letter = newlist

  def FindWordCounts(self):
    #nineletter_counts_file = file(os.path.join(self.options.output_dir,
    #                              'nineletter_counts'), 'w')
    used_words = wordlist.Dict()
    for d,nine_words in self.nineletter_words.iteritems():
      nineletter_counts_file = file(os.path.join(self.options.output_dir,
                                    'nineletterwords_%s' % d), 'w')
      for nine_word in nine_words:
        # words_to_check is list of possible words (those that start
        # with one of the line letters)
        letters_being_checked = []
        words_to_check = wordlist.Dict()
        if d != 'common':
          words_to_check.extend(self.unique_words[d])
        for ltr in sorted(self.common_words_by_letter.keys()):
          if ltr in nine_word and ltr not in letters_being_checked:
            words_to_check.extend(self.common_words_by_letter[ltr])
            letters_being_checked.append(ltr)
            
        new_words_to_check = []
        for word in words_to_check:
            new_words_to_check.append(list(word))
        words_to_check.replace(new_words_to_check)

        counts_letters = {}
        counts = []
        print nine_word,
        nineletter_counts_file.write(nine_word)
        for i in xrange(9):
          letter = nine_word[i]
          #nine_word[i] = nine_word[0]
          #nine_word[0] = letter
          if letter in counts_letters:
            print counts_letters[letter],
            nineletter_counts_file.write('%c' % counts_letters[letter])
            counts.append(counts_letters[letter])
            continue

          word_count = 1
          for w in words_to_check:
            if nine_word[i] not in w:
              continue
            word = list(w)
            candidate_word = w
            #print word
            for ltr in list(nine_word):
              if ltr in word:
                  word.remove(ltr)

            if not word:
              word_count += 1
              used_words.append(candidate_word)
          print word_count,
          if word_count > 255: word_count = 255
          nineletter_counts_file.write('%c' % word_count)
          counts_letters[letter] = word_count
          sys.stdout.flush()
        print
        # nineletter_counts_file.flush()
      nineletter_counts_file.close()

    used_set = set()
    for word in used_words:
      used_set.add(''.join(word))
      
    self.used_words = wordlist.Dict() 
    self.used_words.extend(list(used_set))
    self.used_words.writeToFile(os.path.join(self.options.output_dir, 'used_words'))


def main(argv):
  parser = OptionParser()
  parser.add_option('-d', '--dictionary', dest='original_dictionary',
                    help='Original dictionary files to read, comma separated')
  parser.add_option('-p', '--plurals', dest='plurals_file',
                    help='File of plurals')
  parser.add_option('-l', '--languages', dest='original_languages',
                    help='Languages of original files, comma separated')
  parser.add_option('-o', '--output_dir', dest='output_dir',
                    help='Output directory to write files to.')
  parser.add_option('-n', '--num_nines', dest='num_nines',
                    help='Limit to this many nine letter words.')
  parser.add_option('-c', action='store_true', dest='count',
                    help='Count targets for each 9-letter word.',
                    default=False)
  (options, args) = parser.parse_args()

  dictionaries = Dictionaries(options)
  original_files = zip(options.original_languages.split(','),
                       options.original_dictionary.split(','))
  print 'Reading original files..'
  # dictionaries.plurals = dict.Dict(filename=options.plurals_file)
  # dictionaries.plurals.filterByRegex('^[a-z]{4,9}$')
  for filespec in original_files:
    (language, filename) = filespec
    dictionaries.original_files[language] = filename
    
    lang_dict = wordlist.Dict(filename=filename)
    lang_dict.name = language
    # lang_dict.filterByRegex('^[a-z]{4,9}$')
    # lang_dict.filterByWordsNotInList(dictionaries.plurals)
    # lang_dict.toUpper()
    print '  Read %d words from %s dict.' % (
        len(lang_dict), filename)
    dictionaries.originals[language] = lang_dict

  print 'Done.'
  #print 'Fixing dictionaries...'
  #dictionaries.FixLanguageDictionaries()
  #print 'Done.'
  print 'Extracting common words...'
  dictionaries.GenerateCommonWords()
  print 'Done.'
  print 'Getting nine letters...'
  dictionaries.GetNineLetterWords()
  print 'Done.'
  print 'Making common words by letter...'
  dictionaries.GenerateWordsByLetter()
  print 'Done.'
  print 'Getting word counts...'
  dictionaries.FindWordCounts()
  print 'Done.'
  print 'Removing unused words...'
  dictionaries.RemoveUnusedWords()
  print 'Writing dictionaries...'
  dictionaries.WriteWordFiles(options.output_dir)
  print 'Done.'

if __name__ == '__main__':
  main(sys.argv)
