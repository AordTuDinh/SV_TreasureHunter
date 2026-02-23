#!/bin/bash
USER="ozudo"
PASSWORD="Makomat@1"
mysqldump -u $USER -p$PASSWORD dson > dson:$(date +%d-%m-%Y).sql
mysqldump -u $USER -p$PASSWORD dson_main >dson_main:$(date +%d-%m-%Y).sql



#!/bin/bash

USER="zend"
PASSWORD=""
#OUTPUT="/Users/rabino/DBs"

#rm "$OUTPUTDIR/*gz" > /dev/null 2>&1

databases=`mysql -u $USER -p$PASSWORD -e "SHOW DATABASES;" | tr -d "| " | grep -v Database`

for db in $databases; do
    if [[ "$db" != "information_schema" ]] && [[ "$db" != "performance_schema" ]] && [[ "$db" != "mysql" ]] && [[ "$db" != _* ]] ; then
        echo "Dumping database: $db"
        mysqldump -u $USER -p$PASSWORD --databases $db > `date +%Y%m%d`.$db.sql
       # gzip $OUTPUT/`date +%Y%m%d`.$db.sql
    fi
done