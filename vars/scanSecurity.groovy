def call(body) {
    def settings = [:]

    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = settings
    body()

    container("alpine") {
        // Prepare container
        sh '''
            echo "Install curl and JQ in container"
            apk add curl jq
        '''
        
        // Script retry backoff
        sh '''
            # Project variables

            REPOSITORY=${JOB_NAME%/*}
            TAG=""
            
            if [ $(echo $GIT_BRANCH | grep -E ^developer$) ]; then
                TAG="dev-${GIT_COMMIT:0:10}"
            elif [ $(echo $GIT_BRANCH | grep -E "^(release-.*)|(hotfix-.*)") ]; then
                TAG="${GIT_BRANCH#*-}-${GIT_COMMIT:0:10}"
            elif [ $(echo $GIT_BRANCH | grep -E "v[0-9]\\.[0-9]{1,2}\\.[0-9]{1,3}$") ]; then
                TAG="$GIT_BRANCH"
            fi

            # Harbor variables
            HARBOR_HOST="http://harbor.localhost.com"
            HARBOR_PATH=api/v2.0/projects/gustavome/repositories/${REPOSITORY}/artifacts/${TAG}
            HARBOR_PARAMETERS="with_scan_overview=true"

            HARBOR_URL="${HARBOR_HOST}/${HARBOR_PATH}?${HARBOR_PARAMETERS}"

            # Scripts variables
            MAX_RETRY=10
            COUNT=1
            SLEEP=1
            SEVERITY="null"

            # Script
            while [ "$SEVERITY" == "null"]; do
                HARBOR_RESPONSE=$(curl -X "${HARBOR_URL}" \
                    -H 'accept: application/json' \
                    -H "authorization: Basic ${HARBOR_API_TOKEN}")
                
                SEVERITY=$HARBOR_RESPONSE | jq -r '.scan_overview | to_entries | .[].value.severity'

                echo "Sleep for ${SLEEP} seconds | Count: ${COUNT}"
                sleep $SLEEP
                
                SLEEP=$(($SLEEP*2))

                COUNT=$(($COUNT+1))

                if [ $COUNT -ge MAX_RETRY ]; then
                    echo "Max retry reached"
                    exit 1
                fi
            done

            if ["$SEVERITY" == "Critical"]; then
                echo "Severity is Critical"
                exit 1
            else
                echo "All Good... Severity is $SEVERITY"
            fi
        '''
    }
}