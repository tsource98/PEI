#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import requests, os, redis, json

from rq import Connection, get_current_job, requeue_job, Queue

API = 'http://35.189.102.211/api'
#USER = 'AA-11-AA'
USERENCRYPTED = b'dfPhxwwCSBVME8/iVyM2yqgfm24x5uv8buNlBIkYCaVadW/QDTKYTonpVWUY0EpMtyanyItqQCe1zV+S/4KV4d7Yr4Q='
#DEVICEID = '1'
DEVICEIDENCRYPTED = b'i9sygLphrsTVtnKdVyM2yqgfm24x5uv8buNlBIkYCaVadW/QDTKYTonpVWV55ZGlFthvFXlSDH0TR1B5Ag=='
token = ''

session = requests.Session()
session.auth = (USERENCRYPTED, DEVICEIDENCRYPTED)

def makeLog(title, res):
    currentDir = os.path.dirname(__file__)
    pathFile = os.path.join(currentDir, 'data', 'log.txt')
    with open(pathFile, 'a+') as file:
        file.write("\n\n" + title + "\n" + res)
        
def createToken():
    try:
       tokenGet = session.get(API + "/token")
       jsonToken = json.loads(tokenGet.text)
       global token
       token = jsonToken["token"]
       return token
       
    except:
       print("Error, no token created")

def sendPost(host, dataDict, headers=None):
    my_headers = {'Authorization': 'Bearer ' + token}
    if headers:
        my_headers.update(headers)
        
    res = requests.post(host, json=dataDict, headers=my_headers, timeout=3)
    return res

def sensors(host, dataDict):
    try:
        res = sendPost(host, dataDict)
        
        if (res.text == "Unauthorized Access"):
            tok = createToken()
            res = sendPost(host, dataDict)
            
        
        if res.status_code != 200:
            raise Exception('Response not 200. Res: ' + str(res.status_code) )

        #print(res.status_code)
    #if no connection put back on queue
    except:
        redis_url = os.getenv('REDISTOGO_URL', 'redis://127.0.0.1:6379')
        conn = redis.from_url(redis_url)
        q = Queue("connError", connection=conn)
        job = get_current_job()
        q.enqueue_job(job)

def alertCloud(host, data):
    #makeLog("alertCloud entrada novo", json.dumps(data[0]))
    try:
        dataDict = data[0]
        headers = data[1]
        res = sendPost(host, dataDict, headers)
        
        if (res.text == "Unauthorized Access"):
            tok = createToken()
            res = sendPost(host, dataDict, headers)
        
        if res.status_code != 200:
            raise Exception('Response not 200. Res: ' + str(res.status_code) )
    except:
        redis_url = os.getenv('REDISTOGO_URL', 'redis://127.0.0.1:6379')
        conn = redis.from_url(redis_url)
        q = Queue("connError", connection=conn)

        job = get_current_job()
        q.enqueue_job(job)

def bootTime(host, dataDict):
    try:
        res = sendPost(host, dataDict)
        
        if (res.text == "Unauthorized Access"):
            tok = createToken()
            res = sendPost(host, dataDict)
        if res.status_code != 200:
            raise Exception('Response not 200. Res: ' + str(res.status_code) )
    #if no connection put back on queue
    except:
        redis_url = os.getenv('REDISTOGO_URL', 'redis://127.0.0.1:6379')
        conn = redis.from_url(redis_url)
        q = Queue("connError", connection=conn)
        job = get_current_job()
        q.enqueue_job(job)
        
