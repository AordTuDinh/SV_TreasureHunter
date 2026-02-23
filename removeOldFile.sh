rootDir=('/root/mashi/test' '/root/phuong/test' '/root/tu/test')
for path in "${rootDir[@]}"; do
    #normal logs
    dir=$path/logs
    echo -e "\n\n### Go to '$dir'\n\n"
    cd $dir
    find . -maxdepth 1 -type f -mtime +10 -name "*.log.20*" -print -delete

    #battle logs
    dir=$path/battlelogs
    echo -e "\n\n### Go to '$dir'\n\n"
    cd $dir

    cur_no_dir=`ls -d 20* | wc -l`
    max_no_dir=15
    delta=$((cur_no_dir - max_no_dir))

    echo -e "No of days to be removed: `[ $delta -gt 0 ] && echo $delta || echo 0`"

    idx=0
    if [[ $delta -gt 0 ]]; then
        ls -d 20* | while read dir; do
            echo "Removing '$dir' ..."
            rm -rf "$dir"

            idx=$((idx + 1))
            if [[ $idx -eq $delta ]]; then
                break
            fi
        done
    fi
done