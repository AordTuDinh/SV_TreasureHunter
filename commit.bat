git add *
if [%1]==[] (
    git commit -am "add chuc nang"
) else (
    git commit -am %1
)
git pull
git push
