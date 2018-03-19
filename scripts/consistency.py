"""
    Consistency.py
    ======================
 
    Use it to check possible annotation consistency to review (very roughly without any context).
 
    :Command:
 
    python consistency.py _absolute_path_
 
    Short summary
    -------------------
 
    In Python 2.7, parse each xml files of the inputdirectory to catch all <rs> tag and put their value in a dictionary as the key and classes as the value.
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

def main(args):
    if len(args) == 1:

        #listing files in the directory (args[1] has to be an absolute path)
        try:files = os.listdir(args[0])
        except:
            print "path unfound (be sure to put an absolute path)"
            sys.exit()

        #catch all rs tags
        rs = [] #rs contain all rs values
        sfiles = "" #sfiles is all files in a string variable it serves at line 47
        files = [file_ for file_ in files if ".xml" in file_]

        for file in files:
            with open(args[0] + os.sep + file, 'r') as file_:
                sfiles = sfiles + ''.join(file_.readlines())
            #try:
            tree = ET.ElementTree()
            tree.parse(args[0]+os.sep+file)
            tree = tree.getroot()
            for t in tree.findall("{http://www.tei-c.org/ns/1.0}text"):
                for p in t.findall("{http://www.tei-c.org/ns/1.0}p"):
                    rs = rs + p.findall("{http://www.tei-c.org/ns/1.0}rs")

        #the dictionary "ambiguous" contains the text annotation (string) as key and class (list) as values
        dic = {}
        for elt in rs:
            #if dic contain the token as key add new value else initialize token with first value
            textContent = elt.text.strip()
            if (dic.has_key(textContent)):dic[textContent] = list(set(dic.get(textContent) + [elt.get("type")]))
            else:dic[textContent] = [elt.get("type")]

        #for tok in sorted(dic.keys()):
        #    try:print tok
        #    except:print ""

        for tok in sorted(dic.keys()):
            try:
                #first print part where a dic key has no annotation (while it had at least one somewhere) 
                print tok," ",dic[tok] ,":\n"
                if len(tok)==1:
                    print "\t-> object name too short !","\n"
                    continue
                if tok.isdigit() and len(tok)<4:
                    print "\t-> object name only digits and too short (<4) !","\n"
                    continue 
                regex = '".{0,80}[^>]%s[^<].{0,80}"'%(re.escape(tok))
                result = subprocess.check_output(['egrep -Eo ' + regex + ' ' + args[0] + os.sep + '*.xml; exit 0'], shell=True)
                if (result):
                    print result.replace("\n", "\n\n")
                else:
                    print "\t-> nothing stinky found !","\n"
                
                #then print part where a dic key has multiple value
                if len (dic[tok])>1:
                    print tok," :\n"
                    for class_ in dic[tok]:
                        regex = ur'.{,80}<rs type="%s">%s<.{,80}'%(class_,tok)
                        try:
                            shortexample = re.search(regex, sfiles).group(0).replace("\t","")
                        except:
                            shortexample = "/" #problem with encodage (line 21 opened without utf-8)
                        print class_,": ",shortexample,"\n"
                    print "_____\n"
            except:print "Encoding issue with key token"
    else:
        print "Come on, I need at least 1 argument"
        sys.exit()

if __name__=="__main__":
    main(sys.argv[1:])