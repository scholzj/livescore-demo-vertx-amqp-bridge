# LiveScore service demo with Vert.x AMQP Bridge and Apache Qpid Dispatch

This project demonstrates how can AMQP be used as an API for accessing services. It has a simple service for keeping live scores. The service allows to:
* add games
* update scores
* reguest score results
* receive live score updates

The service is using Apache Qpid Dispatch and Vert.x AMQP Bridge for the AMQP API. Dispatch is used as AMQP server, where AMQP clients can connect. AMQP Bridge in Vert.x connects to it as client and is used to send and receive messages.
  
## API

To send a request, send the message to the specific AMQP address. If reply-to address is specified, the service will send response to it. 

### Add new game

#### Request

* Send a message to endpoint LiveScore/addGame
* The message payload should be in JSON format:
```json
{
  "homeTeam": "Aston Villa", 
  "awayTeam": "Preston North End", 
  "startTime": "14th January 2017, 17:30"
}
```

#### Response

In case of success:

* The response will contain application property `status` with value `201`
* The message payload will contain a JSON object with the created game:
```json
{
  "awayTeam": "Preston North End",
  "awayTeamGoals": 0, 
  "gameTime": "0", 
  "homeTeam": "Aston Villa", 
  "homeTeamGoals": 0, 
  "startTime": "Saturday 14th January 2017, 17:30"
}
```

In case of problems:
* The response will contain application property `status` with value `400`
* The message payload will contain a JSON object with the created game:
```json
{
  "error": "Game between Aston Villa and Preston North End already exists!"
}
```

### Update game score

#### Request

* Send a message to endpoint LiveScore/setScore
* The message payload should be in JSON format:
```json
{
  "homeTeam": "Aston Villa", 
  "awayTeam": "Preston North End", 
  "homeTeamGoals": 1, 
  "awayTeamGoals": 0, 
  "gameTime": "HT"
}
```

#### Response

In case of success:

* The response will contain application property `status` with value `200`
* The message payload will contain a JSON object with the updated game:
```json
{
  "awayTeam": "Preston North End",
  "awayTeamGoals": 1, 
  "gameTime": "HT", 
  "homeTeam": "Aston Villa", 
  "homeTeamGoals": 0, 
  "startTime": "Saturday 14th January 2017, 17:30"
}
```

In case of problems:
* The response will contain application property `status` with value `400`
* The message payload will contain a JSON object with the created game:
```json
{
  "error": "The home and away team goals have to be => 0!"
}
```

### Get game scores

#### Request

* Send a message to endpoint LiveScore/getScores
* The message payload should be empty

#### Response

In case of success:

* The response will contain application property `status` with value `200`
* The message payload will contain a JSON array with the game results:
```json
[
  {
    "awayTeam": "Preston North End",
    "awayTeamGoals": 1, 
    "gameTime": "HT", 
    "homeTeam": "Aston Villa", 
    "homeTeamGoals": 0, 
    "startTime": "Saturday 14th January 2017, 17:30"
  }
]
```

### Receive live score updates

#### Subscribe

* To subscribe to live score updates, connect a receiver to LiveScore/liveUpdates

#### Broadcasts

In case of success:

* The message payload will contain a JSON object with the game results:
```json
{
  "awayTeam": "Preston North End",
  "awayTeamGoals": 1, 
  "gameTime": "HT", 
  "homeTeam": "Aston Villa", 
  "homeTeamGoals": 0, 
  "startTime": "Saturday 14th January 2017, 17:30"
}
```

## Examples

You can use the qpid-send and qpid-receive utilities from Apache Qpid project to communicate with the service from the command line:

* Create a new game
```
qpid-send -b 127.0.0.1:5672 --connection-options "{protocol: amqp1.0}" -a "'LiveScore/addGame'" --content-string '{"homeTeam": "Aston Villa", "awayTeam": "Preston North End", "startTime": "14th January 2017, 17:30"}'

```

* Update the score
```
qpid-send -b 127.0.0.1:5672 --connection-options "{protocol: amqp1.0}" -a "'LiveScore/setScore'" --content-string '{"homeTeam": "Aston Villa", "awayTeam": "Preston North End", "homeTeamGoals": 1, "awayTeamGoals": 0, "gameTime": "13"}'
qpid-send -b 127.0.0.1:5672 --connection-options "{protocol: amqp1.0}" -a "'LiveScore/setScore'" --content-string '{"homeTeam": "Aston Villa", "awayTeam": "Preston North End", "homeTeamGoals": 2, "awayTeamGoals": 0, "gameTime": "35"}'

```

* Get scores
```
$ qpid-receive -b 127.0.0.1:5672 --connection-options "{protocol: amqp1.0}" -a "myReplyToAddress" -m 1 -f --print-headers yes &
$ qpid-send -b 127.0.0.1:5672 --connection-options "{protocol: amqp1.0}" -a "'LiveScore/getScores'" --content-string '{}' --reply-to "myReplyToAddress"

```

* Subscribe to live updates
```
qpid-receive -b 127.0.0.1:5672 --connection-options "{protocol: amqp1.0}" -a "'LiveScore/liveUpdates'" -f

```