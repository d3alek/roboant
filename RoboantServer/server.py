#!/usr/bin/env python

import socket

import pygame
from pygame.locals import *

from twisted.internet import protocol, reactor
from twisted.internet.task import LoopingCall

import qrcode

screenWidth = 400 
screenHeight = 530 

SCREEN_SIZE = (screenWidth, screenHeight)
SERVER_PORT = 1234

name = 'RoboAnt'

pygame.init()

screen = pygame.display.set_mode(SCREEN_SIZE)
pygame.display.set_caption(name)
pygame.joystick.init()
joysticks = [pygame.joystick.Joystick(x) for x in range(pygame.joystick.get_count())]
axisX = 0
axisY = 0
DELTA_MIN = 0

toSendLeftWheelVal = 0
toSendRightWheelVal = 0

print "Joysticks", joysticks

for joystick in joysticks:
    joystick.init()
    print joystick.get_numaxes()
    pass

def findMyIP():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.connect(("gmail.com",80))
    sockname = s.getsockname()[0]
    s.close()
    return sockname


def sendWheels(speedL, speedR):
    if mProtocol != None:
        mProtocol.sendLine('l' + str(int(speedL)) + 'r' + str(int(speedR)))

def moveLeftWheel(speed):
    global toSendLeftWheelVal
    toSendLeftWheelVal = speed
    #toSendLeftWheelVals += [speed]

def moveRightWheel(speed):
    global toSendRightWheelVal
    toSendRightWheelVal = speed
    #toSendRightWheelVals += [speed]

from twisted.protocols.basic import LineReceiver

mProtocol = None

class AntRobotControl(LineReceiver):
    def lineReceived(self, line):
        print line

class AntRobotControlFactory(protocol.Factory):
    def buildProtocol(self, addr):
        global mProtocol
        print "Proto built"
        mProtocol = AntRobotControl() 
        return mProtocol

reactor.listenTCP(SERVER_PORT, AntRobotControlFactory())

axis4 = 0

leftDown = rightDown = False

def events_tick():
    global axisX, axisY, DELTA_MIN, axis4, leftDown, rightDown
    change4 = False
    changeX = False

    for event in pygame.event.get():
        if event.type == QUIT:
            pygame.quit()
            #conn.close()
        elif event.type == MOUSEMOTION:
            mousex, mousey = event.pos
            if mousex < screenWidth / 8:
                if mousey > screenHeight / 2:
                    scrollingLeft = True
            else:
                scrollingLeft = False
            if mousex > 7 * screenWidth / 8:
                if mousey > screenHeight / 2:
                    scrollingRight = True
            else:
                scrollingRight = False

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
            if event.key == K_LEFT:
                leftDown = False
        elif event.type == JOYBUTTONDOWN:
            print "JOYBUTTONDOWN"
            print event.button
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
        elif event.type == JOYBALLMOTION:
            print "JOYBALLMOTION"

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
    else:
        toSendLeftWheelVal = 0
        toSendRightWheelVal = 0



    if leftDown:
        moveLeftWheel(200)
    if rightDown:
        moveRightWheel(200)

def send_tick():
    global toSendRightWheelVal, toSendLeftWheelVal 

    sendWheels(toSendLeftWheelVal, toSendRightWheelVal)

mIP = findMyIP()
qrFile = open('qrIP.png', 'wb')
qrcode.make(mIP + ":" + str(SERVER_PORT)).save(qrFile);
qrFile.close()

def qrcode_tick():
    if not mProtocol:
        screen.fill((0, 0, 0))
        qrImage = pygame.image.load('qrIP.png')
        screen.blit(qrImage, (0, 0))
        pygame.display.update()

    
tick = LoopingCall(events_tick)
tick.start(0.01)

tick2 = LoopingCall(send_tick)
tick2.start(0.3)

tick3 = LoopingCall(qrcode_tick)
tick3.start(1.0)

reactor.run()

#conn.close()


