# Contributing

## Features or Bugs
For any bug fix request or feature request, please create
an [issue](https://github.com/smellouk/kamper/issues/new) for it.

## Requirements
The only requirement is related to platform contribution which you need a respective device for it.

## Gi Convention
Our git convention is inspired by git-flow which has 1 source of truth `develop`.

### Branch naming
* Feature branch: `feature/ISSUE_ID-description_of_your_ticket`, create this branch when you start
  working on task/story branch.
* Bug branch: `bug/ISSUE_ID-description_of_your_bug`
* Release version name should follow [semantic versioning](https://semver.org/), ex: `1.2.3`
  * 1 represents major version
  * 2 represents minor version (which will includes new features and bug fixes)
  * 3 represents patch version (which will include hotfix)
* Experiment branch: `experiment/ISSUE_ID-description_of_your_experiment`

### Commit message
Your commit should follow this format:

```
#ISSUE_ID EMOJI Description of the commit
Use the second line to describe your changes

ex: #123 🐛 Fix crashing app issue
```

Emojis:

- 🐛: Bug
- ⬆️: Version bump
- 🌟: Feature
- 🧪: Tests
- 📦: Publish

Please group your commits by context, you can use `git --amend` to amend new commit with previous
commit or interactive rebase `git rebase -t` to squash the same commits or for removing and editing.

### Tags
your tag naming should follow this format:

```
ex: 1.2.3
```

### Rebase:
Our workflow relies on rebasing instead of merging, whenever you feel your branch is outdated just
rebase it against develop.

### PR Merging
Before creating any PR, please make sure to run on your local machine:
```shell
./gradlew detekt lint test 
```
When merging your PR, you must make sure to use `rebase and merge` and not `create merge commit`, at
At the end of PR merging, your commits must be on the top of the commit's develop.

You can also use `squash and merge` but you need to make sure the commit message formatting is
respected. 