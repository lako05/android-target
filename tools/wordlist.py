#!/usr/bin/python2.4

"""Representation of a dictionary (list) of words."""

import re
import string

class Dict(list):
  """A dictionary.
 
  Attributes:
    filename: Original filename of dictionary
    name: str, Arbitrary name of the list
  """
  def __init__(self, filename=None, name=''):
    super(Dict, self).__init__(self)
    self.filename = filename
    self.name = name
    if filename is not None:
      words = file(filename).readlines()
      # words = map(string.upper, words)
      words = map(string.strip, words)
      self.extend(words)

  def append(self, item):
    """ Append only non-empty values."""
    if len(item):
      super(Dict, self).append(item)
  
  def extend(self, newlist):
    clean_list = []
    for item in newlist:
      if len(item):
        clean_list.append(item)
    super(Dict, self).extend(clean_list)
    
  def replace(self, wordlist):
    del self[:]
    self.extend(wordlist)

  def getWordsByLetter(self):
    word_dict = {}
    for word in self:
      first_letter = word[0]
      if first_letter not in word_dict:
        word_dict[first_letter] = []
      word_dict[first_letter].append(word)
    return word_dict

  def getWordsByLength(self, length):
    wordlist = []
    for word in self:
      if len(word) == length:
        wordlist.append(word)
    return wordlist

  def wordsNotInList(self, wordlist):
    """Fetch words from 'self' that arent also in 'wordlist'."""
    final_list = []
    for word in self:
      if word not in wordlist:
        final_list.append(word)
    return final_list

  def wordsAlsoInList(self, wordlist):
    """Fetch words from 'self' that are also in 'wordlist'."""
    final_list = []
    for word in self:
      if word in wordlist:
        final_list.append(word)
    return final_list

  def filterByRegex(self, regex_str):
    """Filter list by the supplied regex string."""
    new_list = []
    regex = re.compile(regex_str, re.I)
    for word in self:
      if regex.match(word):
        new_list.append(word)
    del self[:]
    self.extend(new_list)
  
  def toUpper(self):
    newlist = []
    for word in self:
      newlist.append(word.upper())
    self.replace(newlist)
    
  def filterByWordsNotInList(self, wordlist):
    """Filter list to words not in the given list."""
    newlist = []
    for word in self:
      if word not in wordlist:
        newlist.append(word)
    self.replace(newlist);

  def writeToFile(self, filename=None):
    """Write all words to 'filename'."""
    if filename is None:
      filename = self.name
    fd = file(filename, 'w')
    for word in self:
      fd.write(word + '\n')
    fd.close()

