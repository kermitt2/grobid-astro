# prend tous les acronymes (sans ceux entre [])
psql -h simrd -d simbad4b -U smbmgr --no-align --no-readline --tuples-only -c "select cat_name from cat where cat_name not like '%[%';" > ids.list 

# decoupe tous les noms propres en gardant que le texte (sans ceux entre [])
# enleve les espaces multiples et decoupe tous les mots en 1 par ligne
# psql -h simrd -d simbad4b -U smbmgr --no-align --no-readline --tuples-only -c "select id from identifier where id like 'NAME %' and id not like 'NAME [%';" | sed 's/[^ [:alpha:]]//g' | sed -e 's/NAME //'|sed -E 's/ +/ /g' | sed -e 's/^ *//' |sed 's/ /\n/g' >> ids.list

# enleve les mots trop petits
cat ids.list | grep -v -E '^[a-Z0-9]{0,2}$' | sort | uniq > lexicon.fromIds

# rajoute tous les noms propres telque
psql -h simrd -d simbad4b -U smbmgr --no-align --no-readline --tuples-only -c "select id from identifier where id like 'NAME %' and id not like 'NAME [%';" | sed -e 's/NAME //' >> lexicon.fromIds

cat /home/oberto/smb4/DJIN/nomenclature/constellations | sed '/^...$/d' >> lexicon.fromIds
#cat /home/oberto/smb4/DJIN/greek_letter >> lexicon.fromIds
