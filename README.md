# Memento Aggregator #

This is LANL's Memento Aggregator software. This software aggregates metadata about Mementos by searching for the given Original-URL across a list of web archives around the world. It is fully compliant with the Memento protocol, and exposes the Memento TimeGate and TimeMap interfaces. The aggregated metadata can be cached in a database for faster future accesses. The aggregator software also provides mechanisms to keep this cache fresh by periodically searching in the web archives for any new mementos. 

The aggregator code consists of three sub-projects:

* Aggregator Core. *TODO: Create documentation in wiki*
* TimeGate & TimeMap Services. *TODO*
* TimeTravel Services. *TODO*

The [wiki](https://bitbucket.org/lanlprototeam/aggregator/wiki/) contains more detailed documentation.

# Compile & Deploy

This page explains how to compile and deploy the aggregator software.

## Code Repository
The aggregator git repo is in bitbucket.

To clone the repo:
`git clone https://bitbucket.org/lanlprototeam/aggregator.git`

## Compile
Requirements:

* Java 1.6+
* Apache Maven

To compile:
`mvn clean install`

A successful compilation will create two JAR files

* `agr-webapp/target/agg*.jar`
* `agg-core/target/agg*.jar`


## Database
### MySQL
Use the schema file provided to create the necessary database and tables for the aggregator. 

The schema file as of 2015-08-19 looks like:
https://www.dropbox.com/s/13n0pd9arb6cky2/agg_tables.sql?dl=0

Update the `agg.properties` file with the appropriate mysql information like user credentials and mysql location.

### Cassandra
Although the aggregator supports cassandra, it is not used in production and hence not covered here.

## Deploy
A Java wrapper from [Tanuki software](http://wrapper.tanukisoftware.com/) is being used for deploying the aggregator. 

==#TODO Provide a link to the wrapper version currently in production, along with the `wrapper.conf` and `agg.properties` files.==

Copy over the 2 agg*.jar files referenced in the "Compile" section above to the `./lib/` folder of the wrapper. Additional jar files that were produced during the compilation (`agr-webapp|agg-core/target/dep/*`) should also be copied to the `./lib/` folder if they are not already there.

Edit the agg.properties file and wrapper.conf file with the appropriate parameters. The most recent file in mementoweb server looks like:
https://www.dropbox.com/s/al8e7sibxpuqicg/agg_mweb.properties?dl=0

The wrapper.conf looks like:
https://www.dropbox.com/s/0j3688tlc00bho1/wrapper.conf?dl=0

Start/stop the aggregator:
`bin/aggapp start`| `bin/aggapp stop`

By default, the aggregator will listen to port 8080, and write to the log file in the directory `logs/wrapper.log`. These can be changed by editing the wrapper config file `conf/wrapper.conf`. 

### nginx Config
The relavant nginx config used in production is shown below. The services that need to be exposed are explicitly mentioned. 

```
server {
	listen 80 ;
	server_name timetravel.mementoweb.org;
    location ~ ^/(list|memento|timegate|redirect|nostalgic)/ {
		proxy_pass       http://localhost:8000;
		proxy_set_header Host $host;
		proxy_set_header X-Real_IP $remote_addr;
	}
```

# Aggregator Workings

## Overview
Every time a request is made for a TimeGate, or a TimeMap, or one of the TimeTravel services, the aggregator faces 4 possible scenarios:

1. Mementos for the requested URL is available in the cache, and the **cache is not stale**.
* Mementos for the requested URL is available in the cache, but the **cache is stale**.
* The requested URL is in the cache, but it has **no Mementos**; no mementos for this URL were found in any of the archives.
* The aggregator is seeing this URL for the first time, and it has **no entry in the cache**.

How the cache works and the conditions under which a cache entry is considered stale is explained in the *"Cache Validity and Periodic Refresh"* Section below.

How the aggregator behaves for each of these scenarios is explained below:

#### Scenario 1 - Cache Hit
This is the ideal situation for the aggregator. When request URL is in the cache, and the cache entry is not stale, the response is prepared and served from the cache.

#### Scenario 2 - Stale Cache

When the request is for a URL in the cache, but the cache entry is stale, the aggregator will perform a dynamic distributed search (See section *Distributed Search*) among a select list of archives. After preparing and responding with the result of this dynamic search, the requested URL is added to the background caching queue for a more thorough search across many more archives. The list of archives for dynamic and thorough searches is dictated by the Rules file (See *Archives and Rules* Section below).

#### Scenario 3 - Cache Hit, No Mementos Found
In the situation when the requested URL is in the cache, but no mementos were found for the URL in any of the archives from an earlier search, the aggregator faces two possibilities:

* If the last search for the URL was performed less than 10 minutes ago, the aggregator will not perform any new searches and simply respond with a HTTP 404.
* If the last search was performed more than 10 minutes ago, the aggregator will follow the steps explained in Scenario 2.

#### Scenario 4 - Cache Miss
When the aggregator is receiving a new URL, it performs the steps explained in Scenario 2.

## Distributed Search
The aggregator will perform a distributed search across archives when facing Scenarios 2, 3 and 4, and also when updating stale cache entries in the background. The list of archives to perform the search is dictated by the Rules file described later. The aggregator performs two kinds of distributed searches:

### Dynamic Search
Dynamic searches typically entail performing TimeGate requests across a select list of archives for the requested URL. This search is usually performed in Scenarios 2, 3, and 4, when the user is waiting for a response and hence, these searches should return responses reasonably quickly. For this reason, only web archives that are natively Memento compatible, and also respond quickly are chosen for these requests. In addition, TimeGate responses are generally fast, and only requires a HTTP HEAD request. The timeout for the search response is set at 30 seconds.

### Thorough Search
The thorough search tries to fetch all the mementos for a requested URL, across all applicable web archives, including CMS's. Since this search tries to collect the complete list of mementos, it takes a long time to complete and has a generous timeout (150 seconds). This is typically intended for background tasks like updating the caches. It may also be used to serve TimeMaps, provided the users wait long enough.

## Aggregator Cache
The Aggregator has a built in cache for storing requested URLs, along with its Memento URLs and Memento Datetimes. The cache improves the response times of known request URLs, and also reduces the load on the hosting server and the web archives. MySQL/MariaDB is the preferred cache backend for the aggregator, although there is (untested) support for Cassandra.


### Cache Validity and Periodic Refresh

A cache entry for a URL is considered **fresh** if it was last updated within the past **30 days**, it is deemed **stale** otherwise. 

The aggregator runs a background process that checks when a URL was last updated in the cache, and performs a thorough search for Mementos if they are stale. This way the chances of a cache hit (Scenario 1) increases, and the aggregator will operate at maximum efficiency. The aggregator's background process adds these stale URLs to a queue so that they can be prioritized and processed based on server load. When updating cache entries, the aggregator performs a thorough distributed search as explained in the *Distributed Search* section above.

#### Cache Update Queues
The aggregator maintains two queues for fetching mementos of URLs; a **regular queue** for updating the stale cache entries, and a **priority queue** for processing requests in Scenarios 2, 3 and 4.

* Priority Queue: Performs a thorough distributed search for any "live requests" that fall under Scenarios 2, 3, and 4. Since these requests are being made by users at the moment, they have a higher priority and will be given first preference when processing. 

* A Regular Queue: Performs a thorough distributed search for the stale URLs in the cache. The URLs in this queue will be processed only when the priority queue is empty.

A pool of threads is allocated to process these queues. Any available thread will first process the URLs from the priority queue, before looking into the regular queue.

### Cache Control Headers
The aggregator and all of its services obey `no-cache` and `only-if-cached` directives of the [Cache-Control](http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.9) header.

#### `Cache-Control: no-cache`
The aggregator faces two possibilities:

* If the cache was last updated in the past 10 minutes, the aggregator will behave as explained above for each of the four scenarios respectively. 
* If the cache was last updated more than 10 minutes ago, the aggregator will behave as explained in Scenario 2 for all cases.

#### `Cache-Control: only-if-cached`
The aggregator will respond from cache, if the URL is available. Otherwise respond with a HTTP 504, Gateway Timeout. This request will not trigger any dynamic distributed searches under any circumstances. For Scenarios 1 and 2, the response will be from cache. For Scenario 3, the response will be a 404, and for Scenario 4 a 504.

## Archives and Rules
The aggregator follows a set of rules to choose the list of archives to query for mementos. The list of archives to poll generally depends on the requested URL and the aggregator service being requested; whether the request is for TimeGate, TimeMap, or TimeTravel related services. It may also depend on the requested date time, but this functionality has not been implemented yet. The basic premise for the rules and the archive list is that, certain archives may have a better collection of mementos for certain kinds of URLs. For example, the UK national web archives may have a better coverage of all URLs that have .uk in the domain name, so we can define a rule in the aggregator to look in this archive for all .uk domains. 

These rules are written in a file in JSON format, and is accessible from the URL: [http://labs.mementoweb.org/aggregator_config/rules.json](http://labs.mementoweb.org/aggregator_config/rules.json). For every request in Scenarios 2, 3, and 4, the aggregator refers to this rules file to determine which archives it should include in its dynamic distributed search.

The Rules file contain the list of archives that should be used for both dynamic search and cache/thorough searches for each of the different aggregator services. For example, the attributes `timetravel_dynamicdefault` lists the archives that should be queried for dynamic timetravel requests, and `timetravel_cachedefault` lists the archives for the thourough timetravel requests.

In addition, the file also lists rules per URL pattern in the array named `rules`. For example, the aggregator can be told to query the GitHub TimeGate only for request URLs that begin with github.com, and not for any other request. If any of these rules is not applicable to a URL, then the aggregator uses the default archive list like `timetravel_dynamicdefault`. 

The archive names listed in this file is the `archive_id` attribute for each archive registered in the central [Archive List XML file](http://labs.mementoweb.org/aggregator_config/archivelist.xml). 

## Database

The MySQL database schema can be found [here](http://timetravel.mementoweb.org/internal/aggregator/). The two main tables the aggregator uses are `links` and `linkmaster`. 

The `linkmaster` table stores information about the original URL, its latest request time, the number of times the url was requested, and the last time this url was updated in the cache. It also contains a unique `id` field which is a MD5 hash of the original url. The protocol in the request url is strippped before storing it and also before computing the `id`. There reasons for that are explained below. This table structure is given below. 
```
+---------+---------------+------+-----+---------+-------+
| Field   | Type          | Null | Key | Default | Extra |
+---------+---------------+------+-----+---------+-------+
| url     | varchar(1024) | YES  |     | NULL    |       |
| id      | char(32)      | NO   | PRI |         |       |
| reqtime | datetime      | YES  |     | NULL    |       |
| numreq  | int(11)       | YES  |     | NULL    |       |
| updtime | datetime      | YES  | MUL | NULL    |       |
| status  | char(1)       | YES  |     | NULL    |       |
+---------+---------------+------+-----+---------+-------+
```

The `links` table stores information on the memento uri, memento datetime, archive, etc for each of the mementos. The `id` of a original url in the `linkmaster` table is used here to identify the original url of a memento. The table looks like:
```
+------------+---------------+------+-----+---------------------+-------+
| Field      | Type          | Null | Key | Default             | Extra |
+------------+---------------+------+-----+---------------------+-------+
| id         | varchar(32)   | NO   | PRI | NULL                |       |
| mdate      | datetime      | NO   | PRI | 0000-00-00 00:00:00 |       |
| archive_id | int(11)       | NO   | PRI | 0                   |       |
| href       | varchar(2050) | YES  |     | NULL                |       |
| type       | varchar(50)   | YES  |     | NULL                |       |
| rel        | varchar(50)   | YES  |     | NULL                |       |
| part       | int(11)       | NO   | PRI | NULL                |       |
| status     | char(1)       | YES  |     | NULL                |       |
+------------+---------------+------+-----+---------------------+-------+
```

NOTE: There are a few fields in this table that were part of the legacy aggregator architecture, that are not being used anymore.

The other table  of importance is `jobs`. This table contains a list of original urls that the aggregator has in it's queue for performing a thorough search. It looks like:
```
+------------+---------------+------+-----+---------+----------------+
| Field      | Type          | Null | Key | Default | Extra          |
+------------+---------------+------+-----+---------+----------------+
| id         | int(11)       | NO   | PRI | NULL    | auto_increment |
| url        | varchar(1024) | YES  |     | NULL    |                |
| reqtime    | datetime      | YES  |     | NULL    |                |
| process_id | int(11)       | YES  | MUL | NULL    |                |
| compltime  | datetime      | YES  |     | NULL    |                |
| priority   | char(1)       | YES  | MUL | NULL    |                |
| hashkey    | char(32)      | YES  | MUL | NULL    |                |
+------------+---------------+------+-----+---------+----------------+
```

### HTTPS/HTTP Protocols
All original urls stored in the `linkmaster` table do not have any protocol information. The protocol (http:// or https://) is stripped from the request url before storing it in the database. The MD5 hash for the `id` is also computed after the protocol is stripped.

The aggregator does not differentiate between http and https resources, even though the HTTP protocol says otherwise. This is done to benefit the end-user, who may not recognise or understand the difference between an http and https resources when searching for their mementos. Hence, the aggregator will be protocol agnostic, and its results will contain both http and https mementos, immaterial of the protocol in the requested url. This means that the aggregator will store the requested url in the `linkmaster` table without the protocol, and the mementos in the `links` table for the requested url may contain both http and https resources. 

## URL Blacklist
The aggregator now checks every requested url and will return an `HTTP 403 Forbidden` error if the url is in a blacklist. This prohibits the aggregator from serving pornographic or malicious content. 

The URL blacklist if provided by [The Unversity of Toulouse 1 Capitole](http://dsi.ut-capitole.fr/blacklists/index_en.php) and contains about 130 million blacklisted domain names in various categories.

### Test urls
```
curl -I http://timetravel.mementoweb.org/api/json/2015/http://Greensboro.com
curl -I  -H   "cache-control: no-cache" http://timetravel.mementoweb.org/timegate/http://poppys-style.com
curl       http://proto1.lanl.gov:9999/timemap/link/1/http://poppys-style.com
curl       http://proto1.lanl.gov:9999/list/2015/http://Greensboro.com
curl  -I  -H 'Accept-Datetime: Sun, 06 Mar 2016 01:19:12 GMT' http://timetravel.mementoweb.org/timegate/http://www.mementoweb.org
```
### Flow charts 
* [The timetravel flow chart ](img/Mementowebtimegate.png)
* [The timetravel timemap flow chart ](img/timemtraveltimemapservice.png)
* [The timetravel cache update flow chart ](img/timetravelbackgr.png)
* [The timetravel loging  chart ](img/timetravelloging.png)
* [The labs timemap flow  chart ](img/labstimemap.png)