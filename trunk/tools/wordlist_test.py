#!/usr/bin/python2.4

import wordlist
import os
import unittest

class TestDict(unittest.TestCase):
  def setUp(self):
    self.dict = wordlist.Dict(filename='dict_test_words.txt')
    self.dict.toUpper()

  def testInit(self):
    self.assertEqual(len(self.dict), 78)
    self.assertEqual(self.dict[10], 'DABBED')
    self.assertEqual(self.dict[-1], 'ZANIES')

  def testGetWordByLetter(self):
    word_dict = self.dict.getWordsByLetter()
    self.assertEqual(word_dict['A'], ['AARDVARK', 'AARDVARKS', 'ABACI'])
    self.assertEqual(word_dict['M'], ['MA', 'MACABRE', 'MACADAM'])
    self.assertEqual(len(word_dict), 26)

  def testWordsNotInList(self):
    newlist = self.dict.wordsNotInList(['AARDVARK', 'ABACI'])
    self.assertEqual(len(newlist), 76)
    self.assertEqual(newlist[0], 'AARDVARKS')
    self.assertEqual(newlist[1], 'BAA')

  def testWordsAlsoInList(self):
    newlist = self.dict.wordsAlsoInList(['FOOBY', 'ABACI', 'COLOUR', 'JABBED',
                                         'JOWLY'])
    self.assertEqual(newlist, ['ABACI', 'JABBED'])

  def testFilterByRegex(self):
    self.dict.filterByRegex('.*ou')
    self.assertEqual(self.dict, ['UBIQUITOUS', 'UBIQUITOUSLY'])

  def testGetWordsByLength(self):
    newlist = self.dict.getWordsByLength(5)
    self.assertEqual(len(newlist), 13)
    self.assertEqual(newlist[0], 'ABACI')
    self.assertEqual(newlist[9], 'RABBI')
    self.assertEqual(newlist[-1], 'YACHT')

  def testWriteToFile(self):
    OUTFILE = '/tmp/dict_test_foo.txt'
    if os.path.exists(OUTFILE):
      os.remove(OUTFILE)
    self.dict.writeToFile(OUTFILE)
    newd = wordlist.Dict(OUTFILE)
    self.assertEqual(len(self.dict), len(newd))
    for i in xrange(len(self.dict)):
      self.assertEqual(self.dict[i], newd[i])
    os.remove(OUTFILE)

  def testAppendExtend(self):
    self.dict.append('ZZYZZ')
    self.assertEqual(self.dict[-1], 'ZZYZZ')

    self.dict.append('')
    self.assertEqual(self.dict[-1], 'ZZYZZ')

    self.dict.extend(['PPOOPP', 'LKLKLK'])
    self.assertEqual(self.dict[-3:], ['ZZYZZ', 'PPOOPP', 'LKLKLK'])

    self.dict.extend(['HGHGH', '', 'MNMN'])
    self.assertEqual(self.dict[-3:], ['LKLKLK', 'HGHGH', 'MNMN'])

  def testFilterByWords(self):
    newdict = wordlist.Dict()
    newdict.extend(['AAA', 'BBB', 'CCC', 'DDD', 'EEE', 'FFF'])
    newdict.filterByWordsNotInList(['CCC', 'FFF'])
    self.assertEqual(newdict, ['AAA', 'BBB', 'DDD', 'EEE'])

if __name__ == '__main__':
  unittest.main()

