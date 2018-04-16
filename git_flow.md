# 列出所有远程主机
git remote
# 参看远程主机的网址
git remote -v
# 查看主机的详细信息
git remote show <主机名>
# 添加远程主机
git remote add <主机名> <网址>
# 删除远程主机
git remote rm <主机名>
# 远程主机的改名
git remote rename <原主机名> <新主机名>



# 将更新取回本地
git fetch <远程主机名>
# 取回特定分支的更新
git fetch <远程主机名> <分支名>

# 取回 origin 主机 next 分支与本地 master 分支合并
git pull origin next:master

# 上述命令等同于
git fetch origin
git merge origin/next

# 查看远程分支
git branch -r
# 查看所有分支
git branch -a

# 删除远程分支
git push origin --delete [branch-name]
git branch -dr [remote/branch]

# 在 origin/master 基础上创建新分支
git checkout -b newBrach origin/master

# 建立追踪关系
# 指定 master 分支追踪 origin/next 分支
git branch --set-upstream master origin/next
# 查看跟踪关系
git branch -vv

# 推送到远程主机
git push <远程主机名> <本地分支名>:<远程分支名>
# 将本地 master 分支推送到 origin 主机，同时指定 origin 为默认主机
git push -u origin master

# git push 默认只推送当前分支
git config --global push.default simple
# 将所有本地分支都推送到 origin 主机
git push --all origin

# 在当前分支上合并 origin/master
git merge origin/master
git rebase origin/master

# Push for Code Review
git push origin HEAD:refs/for/master
# Push with bypassing Code Review
git push origin HEAD:master

# 解决中文乱码
git config --global core.quotepath false
git config --global gui.encoding utf-8
git config --global i18n.commitencoding utf-8
git config --global i18n.logoutputencoding utf-8

# obtain the commit-msg script
scp -p -P 29418 <your username>@<your Gerrit review server>:hooks/commit-msg <local path to your git>/.git/hooks/


# 显示标签的列表和注解
git tag -n
# 列出现有标签
git tag
# 查看 tag 信息
git show [tag]
# 新建一个 tag 在当前 commit
git tag [tag]
# 新建一个 tag 在指定 commit
git tag [tag] [commit]
# 含附注的标签
git tag -a v1.4 -m 'my version 1.4'
# 提交指定tag
git push [remote] [tag]
# 推送所有本地新增的标签
git push [remote] --tags

# 删除本地 tag
git tag -d [tag]
# 删除远程 tag
git push origin :refs/tags/[tagName]

显示包含标签资料的历史记录
git log --decorate
显示简要的增改行数统计
git log --stat
显示每次提交的内容差异
git log -p
展示了每个提交所在的分支及其分化衍合情况
git log --pretty=format:"%h %s" --graph
git log --graph --oneline
显示某个文件的版本历史，包括文件改名
git log --follow [file]
git whatchanged [file]

搜索提交历史，根据关键词
git log -S [keyword]

显示当前分支的最近几次提交
git reflog

显示某个commit之后的所有变动，每个commit占据一行
git log [tag] HEAD --pretty=format:%s
显示某个commit之后的所有变动，其"提交说明"必须符合搜索条件
git log [tag] HEAD --grep feature

显示指定文件是什么人在什么时间修改过
git blame [file]

显示暂存区和工作区的差异
git diff
显示暂存区和上一个commit的差异
git diff --staged [file]
git diff --cached [file]
显示工作区与当前分支最新commit之间的差异
git diff HEAD

显示某次提交的元数据和内容变化
git show [commit]
显示某次提交时，某个文件的内容
git show [commit]:[filename]

git pull从远程仓库拉取代码更易出错，也不利于拉取代码的时候查看代码的改动部分，而git fetch / git merge这组命令可以在git fetch拉取代码后，且在merge代码前有机会查看代码的改动，进而决定需不需要merge到当前分支中来

显示所有远程仓库
git remote -v
添加远程仓库
git remote add [shortname] [url]
可以用字符串 pb 指代对应的仓库地址，抓取所有 pb 有的，但本地仓库没有的信息
git fetch pb
下载远程仓库的所有变动
git fetch [remote]

显示某个远程仓库的信息
git remote show [remote]
git remote show origin
取回远程仓库的变化，并与本地分支合并
git pull [remote] [branch]
上传本地指定分支到远程仓库
git push [remote] [branch]
将master的代码push到master的远程分支
git push origin master
把 pb 改成 paul
git remote rename pb paul
移除对应的远端仓库
git remote rm paul

强行推送当前分支到远程仓库，即使有冲突
git push [remote] --force
推送所有分支到远程仓库
git push [remote] --all


恢复暂存区的指定文件到工作区
git checkout [file]
恢复暂存区的所有文件到工作区
git checkout .
恢复某个commit的指定文件到暂存区和工作区
git checkout [commit] [file]
重置暂存区的指定文件，与上一次commit保持一致，但工作区不变
git reset [file]
重置暂存区与工作区，与上一次commit保持一致
git reset --hard
git reset --hard HEAD~
删除最前面的两个提交
git reset --hard HEAD~~
重置当前分支的指针为指定commit，同时重置暂存区，但工作区不变
git reset [commit]
重置当前分支的HEAD为指定commit，同时重置暂存区和工作区，与指定commit一致
git reset --hard [commit]

安全回退当前提交到工作区
git reset HEAD^
安全地取消过去发布的提交 ?
git revert HEAD

还未提交的修改内容以及新添加的文件，留在索引区域或工作树的情况下切换到其他的分支时，修改内容会从原来的分支移动到目标分支。但是如果在checkout的目标分支中相同的文件也有修改，checkout会失败的。这时要么先提交修改内容，要么用stash暂时保存修改内容后再checkout。
stash是临时保存文件修改内容的区域。stash可以暂时保存工作树和索引里还没提交的修改内容，您可以事后再取出暂存的修改，应用到原先的分支或其他的分支上。
git stash
git stash pop

新创建一个分支用于新的版本1.0，同时也维护着master分支。在master上面发现了bug并将其修复后，也需要在1.0的分支上将这些修改拿过来，此时git cherry-pick <commit ID>就可以了，它将这些修改的commit从master分支上摘取过来，并自动提交到当前的分支

git rebase -i master不仅可以从master同步最新的代码，而且还可以将我们分支上的多个commit合并成为一个，可以让branch更简洁。如果同步时有冲突，我们解决完冲突后，使用git rebase --continue将代码提交到先前的commit中去

汇合提交
git rebase -i HEAD~~
将第二行的“pick”改成“squash”，然后保存并退出

修改提交
git rebase -i HEAD~~
将第一行的“pick”改成“edit”，然后保存并退出
用commit --amend保存修改
git add sample.txt
git commit --amend
通知这个提交的操作已经结束
git rebase --continue


git reset <commit> 的意思就是 把HEAD移到<commit>
soft就是只動repo
git reset --soft <commit>
mixed/default 就是動repo還有staging
git reset --mixed <commit>
hard就是動repo還有staging還有working
git reset --hard <commit>
undo last commit
git reset --soft HEAD~

git merge --no-ff myfeature
The --no-ff flag causes the merge to always create a new commit object, even if the merge could be performed with a fast-forward. This avoids losing information about the historical existence of a feature branch and groups together all commits that together added the feature.
In the latter case, it is impossible to see from the Git history which of the commit objects together have implemented a feature—you would have to manually read all the log messages. Reverting a whole feature (i.e. a group of commits), is a true headache in the latter situation, whereas it is easily done if the --no-ff flag was used.

Release branches are created from the develop branch
git checkout -b release-1.2 develop

git push seraphic HEAD:refs/drafts/$PUSH_BRANCH_NAME
git push seraphic HEAD:refs/for/$PUSH_BRANCH_NAME
git push origin HEAD:refs/for/master











git diff --staged <file>

显示所有远程仓库
git remote -v

git reset --hard HEAD~1
git push --force

重置暂存区
git reset [file]
重置暂存区与工作区
git reset --hard
重置当前分支的指针为指定commit，同时重置暂存区，但工作区不变
git reset [commit]
重置当前分支的HEAD为指定commit，同时重置暂存区和工作区
git reset --hard [commit]
新建一个commit，用来撤销指定commit
git revert [commit]


```sh
git branch develop
git push -u origin develop

git checkout develop
git checkout -b feature_branch

git checkout develop
git merge feature_branch

git checkout develop
git checkout -b release/0.1.0

git checkout develop
git merge release/0.1.0

git checkout master
git checkout -b hotfix_branch

git checkout master
git merge hotfix_branch
git checkout develop
git merge hotfix_branch
git branch -D hotfix_branch
```

```sh
git checkout -b develop
git checkout -b release/0.1.0
git checkout develop
git checkout -b feature_branch
# a bunch of work is done on the features
git checkout release/0.1.0
git merge feature_branch
# assuming that the release branch is done with that feature
git checkout develop
git merge release/0.1.0
git checkout master
git merge release/0.1.0
git branch -D release/0.1.0
```

hotfix
```sh
git checkout master
git checkout -b hotfix_branch
# work is done commits are added to the hotfix_branch
git checkout develop
git merge hotfix_branch
git checkout master
git merge hotfix_branch
```
