#!/usr/bin/env python

import socket

import pygame
from pygame.locals import *

from twisted.internet import protocol, reactor
from twisted.internet.task import LoopingCall

import sys


SCREEN_SIZE = (600, 600)
name = 'RoboAnt'

pygame.init()
clock = pygame.time.Clock()
mSpeed = 0

screen = pygame.display.set_mode(SCREEN_SIZE)
pygame.display.set_caption(name)
pygame.joystick.init()
joysticks = [pygame.joystick.Joystick(x) for x in range(pygame.joystick.get_count())]
axesPosition = {}
axisX = 0
axisY = 0
DELTA_MIN = 0

toSendLeftWheel = False
toSendLeftWheelVal = 0
toSendRightWheel = False
toSendRightWheelVal = 0

toSendLeftWheelVals = []
toSendRightWheelVals = []

print "Joysticks", joysticks

for joystick in joysticks:
    joystick.init()
    print joystick.get_numaxes()
    pass

def sendCameraOn():
    if mProtocol != None:
        mProtocol.sendLine('cam on')

def sendCalibrate():
    if mProtocol != None:
        mProtocol.sendLine('calibrate')

def sendTakePicture():
    if mProtocol != None:
        mProtocol.sendLine('pic')

def sendRecordRoute():
    if mProtocol != None:
        mProtocol.sendLine('rec');

def sendAIOn():
    if mProtocol != None:
        mProtocol.sendLine('ai on');


def findMyIP():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.connect(("gmail.com",80))
    sockname = s.getsockname()[0]
    s.close()
    return sockname

def sendLeftWheel(speed):
    if mProtocol != None:
        mProtocol.sendLine('l ' + str(speed))

def sendRightWheel(speed):
    if mProtocol != None:
        mProtocol.sendLine('r ' + str(speed))

def moveLeftWheel(speed):
    global toSendLeftWheel, toSendLeftWheelVal
    toSendLeftWheel = True
    toSendLeftWheelVal = speed
    #toSendLeftWheelVals += [speed]

def moveRightWheel(speed):
    global toSendRightWheel, toSendRightWheelVal
    toSendRightWheel = True
    toSendRightWheelVal = speed
    #toSendRightWheelVals += [speed]

def moveForward(speed):
    moveLeftWheel(speed)
    moveRightWheel(speed)

def moveBackward(speed):
    moveLeftWheel(-speed)
    moveRightWheel(-speed)

from twisted.protocols.basic import LineReceiver

mProtocol = None

class AntRobotControl(LineReceiver):
    def lineReceived(self, line):
        self.sendLine(line)

class AntRobotControlFactory(protocol.Factory):
    def buildProtocol(self, addr):
        global mProtocol
        print "Proto built"
        mProtocol = AntRobotControl() 
        return mProtocol

reactor.listenTCP(1234, AntRobotControlFactory())


#s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
#print 'Server on', TCP_IP, ":", TCP_PORT
#s.bind((TCP_IP, TCP_PORT))
#s.listen(1)

#conn, addr = s.accept()
#s.settimeout(None)
#print 'Connection address:', addr


axis4 = 0
rightDown = leftDown = False

def events_tick():
    global axisX, axisY, DELTA_MIN, axis4, rightDown, leftDown
    screen.fill(pygame.Color(255, 255, 255)) 
    #print "IMHERE BEFORE RECV"
    #data = conn.recv(BUFFER_SIZE)
    #print "IMHERE AFTER RECV"
#    if not data: 
        #print "NOT DATA"
        #break
    #print "received data:", data
#    #conn.send(data)  # echo changeX = False changeY = False
    change4 = False
    changeX = False

    for event in pygame.event.get():
        if event.type == QUIT:
            pygame.quit()
            #conn.close()
        elif event.type == KEYDOWN:
            if event.key == K_ESCAPE:
                pygame.event.post(pygame.event.Event(QUIT))
            if event.key == K_RIGHT:
                rightDown = True
                #moveLeftWheel(100)
            if event.key == K_LEFT:
                leftDown = True
                #moveRightWheel(100)
#            if event.key == K_UP:
                #change4 = True
                #axis4 += 50
                ##mSpeed += 1
            #if event.key == K_DOWN:
                #change4 = True 
                #axis4 -= 50
            #if event.key == K_RIGHT:
                #changeX = True
                #axisX += 50
            #if event.key == K_LEFT:
                #changeX = True 
                #axisX -= 50
        elif event.type == KEYUP:
            if event.key == K_RIGHT:
                rightDown = False
            if event.key == K_LEFT: leftDown = False
        elif event.type == JOYBUTTONDOWN:
            print "JOYBUTTONDOWN"
            print event.button
            if event.button == 0:
                sendCameraOn()
            if event.button == 10:
                sendAIOn()
            if event.button == 3:
                sendTakePicture()
            if event.button == 1:
                sendRecordRoute()
            if event.button == 9:
                sendCalibrate();
        elif event.type == JOYBUTTONUP:
            print "JOYBUTTONUP" 
            print event.button
        elif event.type == JOYAXISMOTION:
            val = int(event.value * 400)
            print "JOYAXISMOTION", event.axis, val
            if event.axis == 0 and abs(axisX - val) >= DELTA_MIN:
                print "JOYAXISMOTION", event.axis, val
                axisX = val
                changeX = True
            if event.axis == 1 and abs(axisY - val) >= DELTA_MIN:
                print "JOYAXISMOTION", event.axis, val
                axisY = val
                changeY = True
            if event.axis == 2 and abs(axis4 - val) >= DELTA_MIN:
                print "JOYAXISMOTION", event.axis, val
                axis4 = -val
                change4 = True
            #else:
                #print "JOYAXISMOTION", event.axis, val
        elif event.type == JOYBALLMOTION:
            print "JOYBALLMOTION"
                #mSpeed -= 1
    #moveForward(mSpeed)
    if (changeX or change4) and not (axis4 == 0 and not change4):
        if abs(axis4) < DELTA_MIN:
            axis4 = 0
        ratio = 0.5 + axisX / 800.
        #ratio = axisX / 400.
        #left = int((1 + ratio) * axis4)
        #if left > 400:
        #    left = 400 
        #right = int((1 - ratio) * axis4)
        #if right > 400:
        #    right = 400
        #moveLeftWheel(left)
        #moveRightWheel(right)
        moveLeftWheel(int(ratio * axis4))
        moveRightWheel(int((1-ratio) * axis4))

        #else:
            #moveRightWheel(abs(axisY))
        #if changeX:
            #moveRightWheel(abs(axisX))
        #else:
            #moveLeftWheel(abs(axisY))
    elif axis4 == 0:
       moveLeftWheel(axisX/2.) 
       moveRightWheel(-axisX/2.) 

    if leftDown:
        moveLeftWheel(200)
    if rightDown:
        moveRightWheel(200)
    pygame.draw.circle(screen, pygame.Color(0, 255, 0), (axisX+300, axisY+300), 20, 0)
    pygame.display.update()

lastSentRight = 0
lastSentLeft = 0

MAX_SEND_QUEUE_LEN = 10

def send_tick():
    global toSendRightWheelVal, toSendLeftWheelVal 

    sendRightWheel(toSendRightWheelVal)
    sendLeftWheel(toSendLeftWheelVal)
    
tick = LoopingCall(events_tick)
tick.start(0.01)

tick2 = LoopingCall(send_tick)
tick2.start(0.3)

reactor.run()

#conn.close()


