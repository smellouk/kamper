# Contributing

## Features or Bugs

For any bug fix request or feature request, please create
an [issue](https://github.com/smellouk/kamper/issues/new) for it.

## Gi Convention

Our git convention is inspired from git flow which have 1 source of truth `develop`.

### Branch naming

* Feature branch: `feature/ISSUE_ID-description_of_your_ticket`, create this branch when you start
  working on task/story branch.
* Bug branch: `bug/ISSUE_ID-description_of_your_bug`
* Release branch: `release/{platform}/release-version`, create this branch when you start working on
  bug
    * platform should be: `android`, `ios`, ... , ex `release/android/1.2.3`
    * Release version name should follow [semantic versioning](https://semver.org/), ex: `1.2.3`
        * 1 represents major version
        * 2 represents minor version (which will includes new features and bug fixes)
        * 3 represents patch version (which will include hotfix)
* Experiment branch: `experiment/ISSUE_ID-description_of_your_experiment`

### Commit message

Your commit should follow this format:

```
#ISSUE_ID EMOJI Description of the commit
Use second line to describe your changes

ex: #123 🐛 Fix crashing app issue
```

Emojis:

- 🐛: Bug
- ⬆️: Version bump
- 🌟: Feature

Please group your commits by context, you can use `git --amend` to amend new commit with previous
commit or interactive rebase `git rebase -t` to squash same commits or for removing and editing.

### Tags

your tag naming should follow this format:

```
version-platform
ex: 1.2.3-android
```

### Rebase:

Our workflow rely on rebasing instead of merging, when ever you feel your branch is outdated just
rebase it against develop.

### PR Merging

When merging you PR, you must make sure to use `rebase and merge` and not `create merge commit`, at
the end of PR merging, your commits must be on the top of commit's develop.

You can also use `squash and merge` but you need to make sure the commit message formatting is
respected. 