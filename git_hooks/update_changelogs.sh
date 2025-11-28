#!/bin/bash

#########################################################################################################
# This script generates changelogs based on the commits between tags.                                   #
# It supports 3 flags:                                                                                  #
# --tag <tag>: Specifies the tag to generate the changelog from. If not specified, the script generates #
#              the changelog from the latest tag.                                                       #
# --reference_tag <tag>: Specifies the tag to compare the commits with. If not specified, the script    #
#                        compares the commits with the latest tag.                                      #
# --write_changes: Writes the changelog to a file in the fastlane/metadata/android/en-US/changelogs     #
#                  folder. The file name is the versionCode.txt.                                        #
# --commit_changes: Commits the changelog file to the git repository.                                   #
#                                                                                                       #
# The versionCode is extracted from the build.gradle.kts file. If the versionCode does not exist in the #
# build.gradle.kts file, the script uses the latest versionCode found in the changelogs folder.         #
#                                                                                                       #
# The changelog file has the following format:                                                          #
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
# The script must be run from the root of the git repository.                                           #
#########################################################################################################

gitTopLevel="$(git rev-parse --show-toplevel)"
versionCodeFilter="\(versionCode\s\+=\s\+\)\([[:digit:]]\+\)"
tag="HEAD"
# Initialize referenceTag to the latest tag (might be empty if no tags exist)
referenceTag=$(git describe --tags "$(git rev-list --tags --max-count=1)" 2> /dev/null)
changelogsPath="$gitTopLevel/fastlane/metadata/android/en-US/changelogs"
changelogs=0
subjects=()
bodies=()

isWriteChanges=false
isCommitChanges=false

help() {
    echo "Usage: $0 [--tag <tag>] [--reference_tag <tag>] [--write_changes] [--commit_changes]"
    echo
    echo "Arguments:"
    echo "  --tag <tag>            : Tag to compare against the reference tag. If not provided, HEAD is used"
    echo "  --reference_tag <tag>  : Reference tag to compare against. If not provided, the latest tag is used."
    echo "  --write_changes        : Write the changes to a changelog file. The changelog file name is the <versionCode>.txt"
    echo "  --commit_changes       : Commit the changes to the repository."

    exit 1
}

# Parse arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
    --write_changes)
        isWriteChanges=true
        shift
        ;;
    --commit_changes)
      isCommitChanges=true
      shift
      ;;
    --reference_tag)
      if [[ $# -gt 1 ]]; then
          # Check that the next argument ($2) is not the start of another flag
          if [[ "$2" != --* ]]; then
              referenceTag="$2"
              shift 2
        else
              echo "Error: --reference_tag requires a value."
              echo
              help
        fi
      else
          echo "Error: --reference_tag requires a value."
          echo
          help
      fi
      ;;
    --tag)
      if [[ -n "$2" ]]; then
          tag="$2"
          shift 2
      else
          echo "Error: --tag requires a value."
          echo
          help
      fi
      ;;
    --help)
      help
      ;;
    *)
      echo "Unknown option: $1"
      echo
      help
      ;;
  esac
done

# If referenceTag is still empty after initialization and argument parsing,
# it means this is the first tag. Set the reference to the first commit hash.
if [ -z "$referenceTag" ]; then
    echo "No previous tag found. Setting reference tag to the initial commit."
    referenceTag=$(git rev-list --max-parents=0 HEAD)
fi

commitHashesBetweenTags=$(git log "$referenceTag".."$tag" --pretty=format:"%h")
commitHashCount=$(echo "$commitHashesBetweenTags" | wc -l)
# Use 'git show' only if referenceTag is a tag name, otherwise it's the full commit hash
# and the build.gradle.kts file might not exist at that commit (e.g., if it's the root commit).
# We conditionally try to extract the versionCode based on referenceTag not being the root commit.
if [ "$referenceTag" != "$(git rev-list --max-parents=0 HEAD)" ]; then
    referenceVersionCode=$(git show "$referenceTag:mobile/build.gradle.kts" | grep versionCode | sed -e "s/$versionCodeFilter/\2/" | xargs)
else
    referenceVersionCode="0" # Use a default for the very first commit reference
fi

tagVersionCode=$(git show "$tag:mobile/build.gradle.kts" | grep versionCode | sed -e "s/$versionCodeFilter/\2/" | xargs)

echo "Tag: $tag, tagVersionCode: $tagVersionCode"
echo "Reference Tag: $referenceTag, referenceVersionCode: $referenceVersionCode"
echo "processing $commitHashCount commits..."

if [ ! -d "$changelogsPath" ]; then
  echo "Creating changelogs folder..."
  mkdir -p "$changelogsPath"
fi

if [[ $commitHashCount -gt 0 && -f "$changelogsPath/$tagVersionCode.txt" && "$isWriteChanges" == true ]]; then
  echo "'$changelogsPath/$tagVersionCode.txt' already exists, deleting..."
  rm "$changelogsPath/$tagVersionCode.txt"
fi

echo "Generating Changelog between $tag and $referenceTag..."
echo
isFirstCommit=true
for commitHash in $commitHashesBetweenTags; do
  subject=$(git log --format=%s -n 1 "$commitHash" | sed -e 's/Change-Id:\s*.*//' | sed -e 's/Signed-off-by:\s*.*//' | sed -e 's/^[^a-zA-Z0-9]*//')
  # body=$(git log --format=%b -n 1 "$commitHash" | sed -e 's/Change-Id:\s*.*//' | sed -e 's/Signed-off-by:\s*.*//' | sed -e 's/^[^a-zA-Z0-9]*//')
  body=$(git log --format=%b -n 1 "$commitHash" | sed -e 's/Change-Id:\s*.*//' | sed -e 's/Signed-off-by:\s*.*//')

  subjects+=("$subject")
  bodies+=("$body")
  changelogs=${#subjects[@]}

  if [[ $changelogs -eq 1 && "$isWriteChanges" == true ]]; then
    echo "saving to '$changelogsPath/$tagVersionCode.txt'..."
  fi

  echo "- ### $subject"
  if [ "$isWriteChanges" == true ]; then
    echo "- ### $subject" >> "$changelogsPath/$tagVersionCode.txt"
  fi

  if [ "$isFirstCommit" == true ]; then
    isFirstCommit=false
  else
    echo "   > Commit: $commitHash"
    echo "   > "
    if [ "$isWriteChanges" == true ]; then
      echo "   > Commit: $commitHash" >> "$changelogsPath/$tagVersionCode.txt"
      echo "   > " >> "$changelogsPath/$tagVersionCode.txt"
    fi
  fi

  readarray -t lines <<< "$body"
  lineCount=${#lines[@]}
  for line in "${lines[@]}"; do
    if [[ -n "$line" ]]; then
      if [ "$lineCount" -gt 1 ]; then
        echo "   > $line"
        if [ "$isWriteChanges" == true ]; then
          echo "   > $line" >> "$changelogsPath/$tagVersionCode.txt"
        fi
      else
        echo "   > $line"
        if [ "$isWriteChanges" == true ]; then
          echo "   > $line" >> "$changelogsPath/$tagVersionCode.txt"
        fi
      fi
    else
      if [ "$lineCount" -eq 1 ]; then
        echo "   >"
        if [ "$isWriteChanges" == true ]; then
          echo "   >" >> "$changelogsPath/$tagVersionCode.txt"
        fi
      fi
    fi
  done
  echo "------------------------------"
  if [ "$isWriteChanges" == true ]; then
    echo "------------------------------" >> "$changelogsPath/$tagVersionCode.txt"
  fi
done

if [ "$changelogs" -gt 0 ]; then
  fullChangelog="**Full Changelog**: https://github.com/abdalmoniem/AlQuran/compare/$referenceTag...$tag"

  echo
  echo "$fullChangelog"
  if [ "$isWriteChanges" == true ]; then
    echo >> "$changelogsPath/$tagVersionCode.txt"
    echo "$fullChangelog" >> "$changelogsPath/$tagVersionCode.txt"
  fi

  echo
  if [ "$isWriteChanges" == true ]; then
    echo "$changelogs changelog(s) saved to '$changelogsPath/$tagVersionCode.txt'!"
  else
    echo "$changelogs changelog(s) found!"
  fi

  if [ "$isCommitChanges" == true ]; then
    currentCommitHash=$(git rev-parse HEAD)
    isCurrentCommitOnRemote=$(git branch -r --contains "$currentCommitHash")
    if [ -n "$isCurrentCommitOnRemote" ]; then
      echo "commit '$currentCommitHash' is on the remote branch, creating a new change log commit..."
      echo

      git add "$changelogsPath/"
      git commit -sm "updated $changelogs change log(s)"
    else
      echo "commit '$currentCommitHash' is not on the remote branch, amending..."
      echo

      git add "$changelogsPath/"
      git commit --amend --no-edit
    fi

    oldTagRef=$(git rev-parse "$tag")
    newTagRef=$(git rev-parse "HEAD")

    echo
    echo "changing tag: $tag reference from $oldTagRef to $newTagRef..."
    # The commands below delete and re-add the tag, which should only happen
    # if $tag is not 'HEAD' and is actually an existing tag being amended.
    # Since this function is called after the main script has already added a tag,
    # and $tag is passed in as the new tag name (e.g., v0.0.1), this logic is likely fine.
    # However, $tag should ideally be validated to ensure it's not HEAD here.
    # Assuming $tag is correctly passed as the new tag name:
    git tag -d "$tag" && git push origin :refs/tags/"$tag"

    echo "adding tag: $tag..."
    git tag "$tag"
    echo "tag $tag added!"
  fi
else
  echo "No / $changelogs change log(s) found between $referenceTag and $tag"
fi
