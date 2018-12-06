# Working with jira

-jira-data-file = ~/.jira-elv
-max-results = 50

fn set-login [host user pass]{
  echo put" [&host="$host" &user="$user" &pass="$pass"]" >$-jira-data-file
}

fn set-login-pass [host entry]{
  user = "(pass show "$entry" |drop 1 |take 1)"
  pass = "(pass show "$entry" |take 1)"
  set-login $host $user $pass
}

fn get-login {
  use util
  cat $-jira-data-file | each $util:eval~
}

fn -quote-param [p]{
  replaces " " "+" $p
}

fn -issue-url [host key]{
  put $host"/rest/api/2/issue/"$key
}

fn -curl-raw [method path]{
  data = (get-login)
  curl -k -v -u $data[user]:$data[pass] -H 'Content-Type: application/json' \
    -X $method $data[host]$path
}

fn -search-issues-raw [jql @fields]{
  data = (get-login)
  fieldParam = (joins , $fields)
  curl -k -s -u $data[user]:$data[pass] -H 'Content-Type: application/json' \
    $data[host]"/rest/api/2/search?jql="(-quote-param $jql)"&fields="$fieldParam"&maxResults="$-max-results
}

fn -comments-raw [key]{
  data = (get-login)
  curl -k -s -u $data[user]:$data[pass] -H 'Content-Type: application/json' \
    (-issue-url $data[host] $key)"/comment"
}

fn -get-work-raw [key]{
 data = (get-login)
  curl -s -k -u $data[user]:$data[pass] -H 'Content-Type: application/json' \
    -X GET (-issue-url $data[host] $key)"/worklog"
}

fn -create-work-raw [key time @comment]{
  created = (date "+%Y-%m-%dT%H:%m:%S%z")
  data = (get-login)
  req = [&started=$created &timeSpent=$time &comment=(joins " " $comment)]
  echo (put $req | to-json)
  curl -vv -k -u $data[user]:$data[pass] -H 'Content-Type: application/json' \
    -X POST --data (put $req | to-json) (-issue-url $data[host] $key)"/worklog"
}

fn search-issues [jql]{
  -search-issues-raw $jql | from-json | explode (all)[issues] | each [issue]{
    @status-color = (splits - $issue[fields][status][statusCategory][colorName])
    status-color = $status-color[0]

    key = $issue[key]
    prio = $issue[fields][priority][name]
    status = $issue[fields][status][name]
    summary = $issue[fields][summary]
    updated = $issue[fields][updated][:10]
    assignee = ' - '
    if (eq (kind-of $issue[fields][assignee]) "map") {
      assignee = $issue[fields][assignee][name]
    }

    echo (styled $key yellow)" "$assignee" "$prio" "(styled $status $status-color)" "(styled $updated gray)" "$summary
  }
}

fn my-issues [&q= &project=]{
  myself = (get-login)[user]
  query = "assignee="$myself" or reporter="$myself
  if (not-eq "" $q) {
    query = $query" "$q
  } elif (not-eq "" $project) {
    query = $query" AND project="$project
  }
  query = $query" order by updated"
  search-issues $query
}

fn show [key &cn=15]{
  issue = (-search-issues-raw key=$key | from-json)[issues][0]

  @status-color = (splits - $issue[fields][status][statusCategory][colorName])
  status-color = $status-color[0]

  key = $issue[key]
  prio = $issue[fields][priority][name]
  status = $issue[fields][status][name]
  summary = $issue[fields][summary]
  updated = $issue[fields][updated][:10]
  created = $issue[fields][created][:10]
  assignee = '<none>'
  if (eq (kind-of $issue[fields][assignee]) "map") {
    assignee = $issue[fields][assignee][name]
  }
  reporter = $issue[fields][reporter][name]
  descr = $issue[fields][description]

  echo (styled $key yellow)" "$summary
  echo (styled $prio/$status yellow)(styled " "U:$updated" C:"$created" A:"$assignee" R:"$reporter gray)
  echo
  echo $descr | fold -sw 80

  -comments-raw $key | from-json | explode (all)[comments] | take $cn | each [comment]{
    echo (styled –––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––– blue)
    echo (styled $comment[updated]" "$comment[author][name] blue)
    echo
    echo $comment[body] | fold -sw 80
  }
}
