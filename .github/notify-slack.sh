#!/usr/bin/env bash

PRIVATE_KEY=$1
USER=$2
PAYLOAD=$3
WEBHOOK_URL=$4

function notify() {
    tempfolder=$(mktemp -d)
    cd ${tempfolder}
    keyfile=$(mktemp)

    cat <<< "${PRIVATE_KEY}" > "${keyfile}"

    GIT_SSH_COMMAND="ssh -i ${keyfile} -o IdentitiesOnly=yes" git clone git@github.com:navikt/pensjon-github-to-slack-username.git 2>&1
    rm ${keyfile}
    res=$(grep ${USER} pensjon-github-to-slack-username/brukernavnoversikt.csv)
    status=$?

    if [[ $status -eq 0 ]]; then
            IFS="," read -r github slack slack_id team <<< "${res}"
            slack_id="<@$slack_id>"
    else
            slack_id=${USER}
            team=unknown
    fi

    export slack_id
    export team

    PAYLOAD=$(envsubst <<< "${PAYLOAD}")
    curl -X POST --data-urlencode "${PAYLOAD}" $WEBHOOK_URL
}

(notify)
