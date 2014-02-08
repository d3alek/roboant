#!/usr/bin/env python

import socket

import pygame
from pygame.locals import *

from twisted.internet import protocol, reactor
from twisted.internet.task import LoopingCall

import sys


screenWidth = 1200
screenHeight = 750
imageHeight = screenHeight / 4
#imageWidth = int(imageHeight * 1.3)
imageWidth = imageHeight

SCREEN_SIZE = (screenWidth, screenHeight)
SPHERE_RAD = 450

scrollX = 0

mTurnSSD = {}
mRouteSSD = {}

#def imagePosition(num):
    #global scrollX
    #return (num * imageWidth - scrollX, imageHeight)

import numpy as np

def imagePosition(deg):
    global scrollX, SPHERE_RAD
    deg = 4*deg/3
    pos = (screenWidth/2 - imageWidth/2 + np.sin(np.radians(deg))*SPHERE_RAD, screenHeight/2-np.cos(np.radians(deg))*SPHERE_RAD)
    pos = (int(pos[0]),int(pos[1]) + 80)
    return pos

def imageGoTowardsPosition(pos):
    global scrollX
    #x = screenWidth/2 - imageWidth/2 + pos * imageWidth - scrollX
    x = pos * imageWidth - scrollX
    y = screenHeight - imageHeight
    return (x, y)

picturesChanged = True

name = 'RoboAnt'

images = pygame.Surface((screenWidth, screenHeight))

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

def sendGoMessage():
    if mProtocol != None:
        mProtocol.sendLine('go')

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
class Images: GoTowards, LookAround = range(2) 

MESSAGE_NEW_LOOK_AROUND = "new_look_around"
MESSAGE_TURN_TO = r'turn_to\s(-?\d+)' 
MESSAGE_IMAGE = r'picture start\s(gotowards\s)?(-?\d+)' 
MESSAGE_ROUTE_MATCH = r'route_match\s(\d+)'
MESSAGE_SSD = r'ssd\s(-?\w+)\s(\w+)\s(\S+)'

toHighlightTurn = None 
toHighlightRoute = None 

def deleteCurrentImages():
    global mTurnSSD, mRouteSSD
    for fname in os.listdir('.'):
        if re.search(r'image_(-?\d+)', fname):
            os.remove(os.path.join('.', fname))
    #mTurnSSD = {}
    mRouteSSD = {}
        

class AntRobotControl(LineReceiver):
    delimiter = '\n'
    imgData = ""
    pictureEnd = "picture end\n"
    pictureStart = "picture start"
    gotowardsEnd = "gotowards"

    def lineReceived(self, line):
        #self.sendLine(line)
        global toHighlightTurn, toHighlightRoute, mTurnSSD, mRouteSSD
        print line
        #if line.startswith(self.pictureStart):
            #if line.endswith(self.gotowardsEnd):
                #self.imageType = Images.GoTowards
            #else:
                #self.imageType = Images.LookAround
                #self.imageNum = int(line.split()[-1]) 
            #self.setRawMode()
            #self.imgData = ""
        m = re.search(MESSAGE_IMAGE, line)
        if m:
            if m.group(1):
                self.imageType = Images.GoTowards
            else:
                self.imageType = Images.LookAround

            self.imageNum = int(m.group(2))
            self.setRawMode()
            self.imgData = ""

        m = re.search(MESSAGE_ROUTE_MATCH, line)
        if m:
            toHighlightRoute = int(m.group(1))
            print 'Route match ', str(toHighlightRoute)

        if line == MESSAGE_NEW_LOOK_AROUND:
            toHighlightTurn = None
            deleteCurrentImages()
        m = re.search(MESSAGE_TURN_TO, line)
        if  m:
            toHighlightTurn = int(m.group(1))

        m = re.search(MESSAGE_SSD, line)
        if m:
            deg = int(m.group(1))
            routeNum = int(m.group(2))
            ssd = int(float(m.group(3)))
            print "SSD", str(deg), str(routeNum), str(ssd)
            if mTurnSSD.has_key(deg):
                mTurnSSD.clear()
            mTurnSSD[deg] = (ssd, routeNum)
            mRouteSSD[routeNum] = (ssd, deg)


    def rawDataReceived(self, data):
        global picturesChanged
        if data.endswith(self.pictureEnd):
            self.imgData += data[:-len(self.pictureEnd)]
            f = ""
            if self.imageType == Images.LookAround:
                f = open('image_' + str(self.imageNum) + '.jpeg', 'wb')
            else:
                f = open('image_go_towards_' + str(self.imageNum) + '.jpeg', 'wb')
            print "Picture end for ", str(self.imageNum), " found"
            f.write(self.imgData)
            f.close()
            self.setLineMode();
            picturesChanged = True

        else:
            self.imgData += data


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

import os, re

maxImageNum = 0

mFont = pygame.font.Font(None, 36)

textColor = (0, 160, 0)

def draw_graph():
    global mTurnSSD

    
    screen.fill(pygame.Color(0, 0, 0))

    points = []
    for key in sorted(mTurnSSD.keys()):
        #points += [(key * 5 + screenWidth/2, mTurnSSD[key][0]/100000)]
        points += [(key * 5, mTurnSSD[key][0]/100000)]
        #pygame.draw.circle(screen, (255, 255, 255), (key*5 + screenWidth/2,
                                                     #mTurnSSD[key][0]/100000), 20, 0)
    if len(points) > 1:
        pygame.draw.lines(screen, (255, 255, 255), False, points, 2)

    pygame.display.update()

def images_tick():
    global imageGoTowardsPosition, picturesChanged, imageSize
    global maxImageNum, toHighlightTurn, toHighlightRoute
    global mTurnSSD, mRouteSSD
    global mFont
    global textColor

    images.fill(pygame.Color(0, 0, 0))

    routeImageNums = []
    turnImageNums = []
    maxImageNum = 0


    for fname in os.listdir('.'):
        m = re.search(r'image_(go_towards_)?(-?\d+)', fname)
        if not m:
            continue

        imageSurface = pygame.image.load(fname)
        imageNum = int(m.group(2))


        if m.group(1):
            imageSurface = pygame.transform.scale(imageSurface,
                                                  (imageWidth, imageHeight))
            images.blit(imageSurface, imageGoTowardsPosition(imageNum))
            if imageNum > maxImageNum:
                maxImageNum = imageNum
            routeImageNums += [imageNum]
        else:
            imageSurface = pygame.transform.scale(imageSurface,
                                                  (imageWidth,imageHeight))
            images.blit(imageSurface, imagePosition(imageNum))

            turnImageNums += [imageNum]

    for num in routeImageNums:
        if mRouteSSD.has_key(num):
            text = mFont.render(mRouteSSD[num][0] + ', ' + str(mRouteSSD[num][1]), 1, textColor)
            images.blit(text, imageGoTowardsPosition(num))

    for num in turnImageNums:
        if mTurnSSD.has_key(num):
            text = mFont.render(mTurnSSD[num][0] + ", " + str(mTurnSSD[num][1]), 1, textColor)
            images.blit(text, imagePosition(num))


    screen.blit(images, (0, 0))


    if toHighlightTurn != None:
        pos = imagePosition(toHighlightTurn)
        pos = (pos[0] + imageWidth/2, pos[1] + imageHeight/2)
        pygame.draw.circle(screen, pygame.Color(255, 0, 0), pos, 20, 0)

    if toHighlightRoute != None:
        pos = imageGoTowardsPosition(toHighlightRoute)
        pos = (pos[0] + imageWidth/2, pos[1] + imageHeight/2)
        pygame.draw.circle(screen, pygame.Color(0, 255, 0), pos, 20, 0)


    pygame.display.update()

scrollingLeft = scrollingRight = False


def events_tick():
    global axisX, axisY, DELTA_MIN, axis4, rightDown, leftDown
    global maxImageNum, scrollX, scrollingLeft, scrollingRight
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
            if event.key == K_g:
                sendGoMessage();
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
    #pygame.draw.circle(screen, pygame.Color(0, 255, 0), (axisX+300, axisY+300), 20, 0)
    if scrollingLeft:
        if scrollX > 0:
            scrollX -= 20
        else:
            scrollingLeft = False
    elif scrollingRight:
        if scrollX < (maxImageNum + 3.5) * imageWidth:
            scrollX += 20
        else:
            scrollingRight = False

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

#tick3 = LoopingCall(images_tick)
#tick3.start(1/10.)

tick4 = LoopingCall(draw_graph)
tick4.start(1);


reactor.run()

#conn.close()


