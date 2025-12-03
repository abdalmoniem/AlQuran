#!/bin/bash

#########################################################################################################
# This script generates changelogs for each release tag in the repository.                              #
#                                                                                                       #
# The script generates changelogs by comparing the commits between each tag.                            #
# It starts from the latest tag and goes back to the previous tag. The                                  #
# script extracts the versionCode from the build.gradle.kts file at the                                 #
# tag commit. It then generates a changelog for the commits between the                                 #
# tag and the previous tag.                                                                             #
#                                                                                                       #
# The generated changelog consists of a list of subjects of the commits                                 #
# between the tag and the previous tag.                                                                 #
#                                                                                                       #
# It supports 3 flags:                                                                                  #
#   --write_changes        : Write the changes to a changelog file. The                                 #
#                            changelog file name is the <versionCode>.txt                               #
#   --commit_changes       : Commit the changes to the repository.                                      #
#                                                                                                       #
# The script generates the changelogs in the following directory:                                       #
# <gitTopLevel>/fastlane/metadata/android/en/changelogs                                              #
#                                                                                                       #
# The changelog files has the following format:                                                         #
#                                                                                                       #
# - ### <subject1>                                                                                      #
#   > <commit hash1>                                                                                    #
#   > <commit body1>                                                                                    #
# - ### <subject2>                                                                                      #
#   > <commit hash2>                                                                                    #
#   > <commit body2>                                                                                    #
#   > ...                                                                                               #
#                                                                                                       #
# **Full Changelog**: https://github.com/abdalmoniem/AlQuran/compare/<referenceTag>...<tag>             #
#                                                                                                       #
# The changelogs are generated as follows:                                                              #
# - in reverse order, starting from the latest tag.                                                     #
# - for all tags in the repository, including the initial tag.                                          #
# - even if the tag is the first commit in the repository.                                              #
#########################################################################################################

gitTopLevel="$(git rev-parse --show-toplevel)"
versionCodeFilter="\(versionCode\s\+=\s\+\)\([[:digit:]]\+\)"
changelogsPath="$gitTopLevel/fastlane/metadata/android/en/changelogs"
# shellcheck disable=SC2207
tags=($(git tag))
changelogs=0

isWriteChanges=false
isCommitChanges=false

help() {
    echo "Usage: $0 [--write_changes] [--commit_changes]"
    echo
    echo "Arguments:"
    echo "  --write_changes        : Write the changes to a changelog file. The changelog file name is the <versionCode>.txt"
    echo "  --commit_changes       : Commit the changes to the repository."

    exit 1
}

# Parse command-line flags
while [[ $# -gt 0 ]]; do
  case "$1" in
    --write_changes)
      isWriteChanges=true
      shift # Move to the next argument
      ;;
    --commit_changes)
      isCommitChanges=true
      shift # Move to the next argument
      ;;
    --help)
      help
      ;;
    *)
      echo "Unknown option: $1"
      help
      ;;
  esac
done

if [[ "$isWriteChanges" == true ]]; then
  if [ ! -d "$changelogsPath" ]; then
    echo "Creating changelogs folder..."
    mkdir -p "$changelogsPath"
  fi
fi

totalChangeLogs=0
for index in $(seq $((${#tags[@]} - 1))   -1 0); do
  tag="${tags[$index]}"

  # Get the previous tag, or the first commit hash if index is 0
  if ((index > 0)); then
    previousTag="${tags[$((index - 1))]}"
  else
    previousTag=$(git rev-parse --short "$(git rev-list --max-parents=0 HEAD)")
  fi

  echo "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++"
  versionCode=$(git show "$tag:mobile/build.gradle.kts" | grep -e "versionCode\s*=\s*" | sed -e "s/$versionCodeFilter/\2/" | xargs)

  commitHashesBetweenTags=$(git log "$previousTag".."$tag" --pretty=format:"%h")
  commitHashCount=$(echo "$commitHashesBetweenTags" | wc -l)

  if [[ $commitHashCount -gt 0 && -f "$changelogsPath/$versionCode.txt" && "$isWriteChanges" == true ]]; then
    echo "'$changelogsPath/$versionCode.txt' already exists, deleting..."
    rm "$changelogsPath/$versionCode.txt"
  fi

  echo "Generating Changelog between $tag and $previousTag..."
  echo "processing $commitHashCount commits..."
  echo
  for commitHash in $commitHashesBetweenTags; do
    subject=$(git log --format=%s -n 1 "$commitHash" | sed -e 's/Change-Id:\s*.*//' | sed -e 's/Signed-off-by:\s*.*//' | sed -e 's/^[^a-zA-Z0-9]*//')
    # body=$(git log --format=%b -n 1 "$commitHash" | sed -e 's/Change-Id:\s*.*//' | sed -e 's/Signed-off-by:\s*.*//' | sed -e 's/^[^a-zA-Z0-9]*//')
    body=$(git log --format=%b -n 1 "$commitHash" | sed -e 's/Change-Id:\s*.*//' | sed -e 's/Signed-off-by:\s*.*//')

    subjects+=("$subject")
    bodies+=("$body")
    changelogs=${#subjects[@]}

    if [[ $changelogs -eq 1 && "$isWriteChanges" == true ]]; then
      echo "saving to '$changelogsPath/$versionCode.txt'..."
    fi

    echo "- ### $subject"
    if [ "$isWriteChanges" == true ]; then
      echo "- ### $subject" >> "$changelogsPath/$versionCode.txt"
    fi

    echo "   > Commit: $commitHash"
    echo "   > "
    if [ "$isWriteChanges" == true ]; then
      echo "   > Commit: $commitHash" >> "$changelogsPath/$versionCode.txt"
      echo "   > " >> "$changelogsPath/$versionCode.txt"
    fi

    readarray -t lines <<< "$body"
    lineCount=${#lines[@]}
    for line in "${lines[@]}"; do
      if [[ -n "$line" ]]; then
        if [ "$lineCount" -gt 1 ]; then
          echo "   > $line"
          if [ "$isWriteChanges" == true ]; then
            echo "   > $line" >> "$changelogsPath/$versionCode.txt"
          fi
        else
          echo "   > $line"
          if [ "$isWriteChanges" == true ]; then
            echo "   > $line" >> "$changelogsPath/$versionCode.txt"
          fi
        fi
      else
        if [ "$lineCount" -eq 1 ]; then
          echo "   >"
          if [ "$isWriteChanges" == true ]; then
            echo "   >" >> "$changelogsPath/$versionCode.txt"
          fi
        fi
      fi
    done
    echo "------------------------------"
    if [ "$isWriteChanges" == true ]; then
      echo "------------------------------" >> "$changelogsPath/$versionCode.txt"
    fi
  done

  if [ "$changelogs" -gt 0 ]; then
    ((totalChangeLogs++))

    if [[ -n "$previousTag" ]]; then
      fullChangelog="**Full Changelog**: https://github.com/abdalmoniem/AlQuran/compare/$previousTag...$tag"
    else
      fullChangelog="**Full Changelog**: https://github.com/abdalmoniem/AlQuran/commits/$tag"
    fi

    echo
    echo "$fullChangelog"
    if [ "$isWriteChanges" == true ]; then
      echo >> "$changelogsPath/$versionCode.txt"
      echo "$fullChangelog" >> "$changelogsPath/$versionCode.txt"
    fi

    echo
    if [ "$isWriteChanges" == true ]; then
      echo "$changelogs changelog(s) saved to '$changelogsPath/$versionCode.txt'!"
    else
      echo "$changelogs changelog(s) found!"
    fi
  fi

  subjects=()
  bodies=()
done

if [ $totalChangeLogs -gt 0 ]; then
  if [ "$isWriteChanges" == true ]; then
      echo "$totalChangeLogs total changelog(s) saved!"
    else
      echo "$totalChangeLogs total changelog(s) found!"
    fi

  if [ "$isCommitChanges" == true ]; then
    currentCommitHash=$(git rev-parse HEAD)
    isCurrentCommitOnRemote=$(git branch -r --contains "$currentCommitHash")

    echo

    if [ -n "$isCurrentCommitOnRemote" ]; then
      echo "commit '$currentCommitHash' is on the remote branch, creating a new change log commit..."
      echo

      git add "$changelogsPath"
      git commit -sm "updated $changelogs change logs(s)"
    else
      echo "commit '$currentCommitHash' is not on the remote branch, amending..."
      echo

      git add "$changelogsPath"
      git commit --amend --no-edit
    fi
  fi
fi
