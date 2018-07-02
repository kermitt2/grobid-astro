"""
    Consistency.py
    ======================
 
    Use it to check possible annotation consistency to review (very roughly without any context).
 
    :Command:
 
    python consistency.py _absolute_path_to_training_directory_
 
    Short summary
    -------------------
 
    In Python 3.*, parse each xml files of the input directory to catch all <rs> tag and put their value in a dictionary as the key and classes as the value.
    First check if the string value occurs somewhere in the corpus not annotated (rough/overgenerating estimation, in particular for super short entities
    but it should be useful enough to point to this type of unconsistency)
    Second it just shows all keys with multiple class values (with short example for each values).
 
    Example of results
    ------------------------

    ** suspicious non annotation 

    WR102   ['astro-object'] :

    /home/lopez/grobid/grobid-astro/resources/dataset/astro/corpus//2016MNRAS.460.4038E.tei.xml: 
    or higher than 32 M~ assuming the rotational models. The rotational velocity of WR102 is actually unknown. Although Sander, Hamann &amp; Todt (2012) report on a very 

    W0607+24   ['astro-object'] :

    /home/lopez/grobid/grobid-astro/resources/dataset/astro/corpus//2016AJ....152..123G.tei.xml:
    .6 hr; the average is 7.5 hr. We therefore argue that most likely we are viewing W0607+24 close to pole-on. This also explains the lack of variability in the two Spitzer     

    ** suspecious multiple classes

    United States Holocaust Memorial Museum  :

    INSTALLATION :  <sentence xml:id="P153E0">The <rs type="INSTALLATION">United States Holocaust Memorial Museum</rs> provides the account of <rs type="MEASURE">one</rs> survivor of 

    INSTITUTION :  <sentence xml:id="P472E2">According to the <rs type="INSTITUTION">United States Holocaust Memorial Museum</rs>, <rs type="LOCATION">Washington, D.C.</rs>, &quot;The fate of < 

    _____

    2015 general election  :

    EVENT :  <sentence xml:id="P22E2">At the <rs type="EVENT">2015 general election</rs>  <rs type="ORGANISATION">UKIP</rs> took <rs type="MEASURE"> 

    PERIOD :   <rs type="ORGANISATION">Labour Party</rs>&apos;s position prior to the <rs type="PERIOD">2015 general election</rs> under <rs type="PERSON">Miliband</rs>, acting <rs type="ORG 

 
"""

import sys
import os
import xml.etree.ElementTree as ET
import re
import subprocess

# for making console output less boring
green = '\x1b[32m'
red = '\x1b[31m'
bold_red = '\x1b[1;31m'
orange = '\x1b[33m'
white = '\x1b[37m'
blue = '\x1b[34m'
score = '\x1b[7m'
bright = '\x1b[1m'
bold_yellow = '\x1b[1;33m'
reset = '\x1b[0m'


def main(args):
    if len(args) == 1:

        # listing files in the directory (args[1] has to be an absolute path)
        try:
            files = os.listdir(args[0])
        except:
            print("path unfound (be sure to put an absolute path):")
            sys.exit()

        # catch all rs tags
        rs = [] # rs contain all rs values
        sfiles = "" # sfiles is all files in a string variable it serves at line 127
        files = [file_ for file_ in files if file_.endswith(".xml") ]
        #print(files)
        for file in files:
            with open(args[0] + os.sep + file, 'r') as file_:
                sfiles = sfiles + ''.join(file_.readlines())
            #try:
            tree = ET.ElementTree()
            tree.parse(args[0]+os.sep+file)
            tree = tree.getroot()
            for t in tree.findall(u"{http://www.tei-c.org/ns/1.0}text"):
                for p in t.findall(u"{http://www.tei-c.org/ns/1.0}p"):
                    rs = rs + p.findall(u"{http://www.tei-c.org/ns/1.0}rs")

        # the dictionary "ambiguous" contains the text annotation (string) as key and class (list) as values
        dic = {}
        for elt in rs:
            # if dic contain the token as key add new value else initialize token with first value
            textContent = elt.text.strip()
            #print(textContent)
            if textContent in dic:
                dic[textContent] = list(set(dic.get(textContent) + [elt.get("type")]))
            else:
                dic[textContent] = [elt.get("type")]
        """
        for tok in sorted(dic.keys()):
            try: 
                print(tok)
            except: 
                print("")
        """
        for tok in sorted(dic.keys()):
            try:
                # first print part where a dic key has no annotation (while it had at least one somewhere) 
                print(bold_yellow, tok, reset, " ", dic[tok],  ":\n")
                if len(tok) == 1:
                    print("\t-> object name too short !","\n")
                    continue
                if tok.isdigit() and len(tok)<4:
                    print("\t-> object name only digits and too short (<4) !","\n")
                    continue 
                regex = '".{0,80}'+'[^>]{}[^<]'.format(re.escape(tok))+'.{0,80}"'
                #print(regex)
                #print("sheel command:", 'egrep -Eo ' + regex + ' ' + args[0] + os.sep + '*.xml; exit 0')
                result = subprocess.getoutput(['egrep -Eo ' + regex + ' ' + args[0] + os.sep + '*.xml; exit 0'])
                if (result):
                    res = result.replace("\n", "\n\n")
                    res = res.replace(tok, bold_red + tok + reset)
                    print(res, "\n")
                else:
                    print("\t-> nothing stinky found !","\n")
                
                #then print part where a dic key has multiple value
                if len (dic[tok])>1:
                    print(tok, " :\n")
                    for class_ in dic[tok]:
                        regex = '".{,80}'+'<rs type="{}">{}<'.format(class_, tok)+'.{,80}"'
                        try:
                            shortexample = re.search(regex, sfiles).group(0).replace("\t","")
                        except:
                            shortexample = "/" #problem with encodage (line 21 opened without utf-8)
                        print(class_, ": ", shortexample, "\n")
                    print("_____\n")
            except:
                print("Issue with key token:", tok)
    else:
        print("Come on, I need at least 1 argument")
        sys.exit()

if __name__=="__main__":
    main(sys.argv[1:])
