#!/usr/bin/python
import subprocess

def call_pass(name):
    p = subprocess.Popen(["pass", "show", name], stdout=subprocess.PIPE)
    return p.stdout.read()

def get_password_pass(name):
    return call_pass(name).split('\n', 1)[0]

def get_user_pass(name):
    return findValue('user', call_pass(name).split('\n'))

def byKey(key):
    return lambda x: x.startswith(key + ':')

def getValue(key):
    return lambda x: x.lstrip(key +':').strip()

def findValue(key, lines):
    return map(getValue(key), filter(byKey(key), lines))[0]

def get_host_pass(name):
    return findValue('mailhost', call_pass(name).split('\n'))

def get_port_pass(name):
    return int(findValue('imap-port', call_pass(name).split('\n')))
