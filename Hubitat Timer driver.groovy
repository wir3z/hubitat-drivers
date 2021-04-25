/**
 *  Copyright 2020 Lyle Pakula
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Timer Driver
 *
 *  Author: Lyle Pakula (lpakula)
 *  Date: 2021-04-24
 *
 *
 */

metadata {
    definition (name: "Timer", namespace: "lpakula", author: "Lyle Pakula")  {
        capability "TimedSession"
        capability "Refresh"
        
        command    "startTimer", ["number"]

        attribute  "timeRemaining", "number"
        attribute  "sessionStatus", "enum"	         // ENUM ["stopped", "canceled", "running", "paused"]
        attribute  "errorCode", "enum"	             // ENUM ["noTimerExists", "timerValueOutOfRange", "aboveMaximumTimerDuration", "belowMinimumTimerDuration"]
    }

    preferences {
        input(name: "maxTimerLimitSec", type: "integer", title: "Maximum Timer Value (seconds)", required: true, displayDuringSetup: true, defaultValue: 86400)        
    }
}

def refresh() {
}

def installed() {
    updated()
}

def updated() {
	unschedule()
    state.sessionStatus = ["stopped", "canceled", "running", "paused"]
    state.errorCode = ["no error","noTimerExists", "timerValueOutOfRange", "aboveMaximumTimerDuration", "belowMinimumTimerDuration"]
    stop()      
    refresh()
}

def cancel() {
    if (isTimerIdle()) {
        // no timer running
        sendEvent(name: "errorCode", value: state.errorCode[1], display: true, displayed: true)
    } else { 
        log.info "cancel"        
        sendEvent(name: "sessionStatus", value: state.sessionStatus[1], display: true, displayed: true)
        resetTimer() 
    }        
}

def pause() {
    if (isTimerIdle()) {       
        // no timer running
        sendEvent(name: "errorCode", value: state.errorCode[1], display: true, displayed: true)
    } else {   
        log.info "pause"        
        sendEvent(name: "sessionStatus", value: state.sessionStatus[3], display: true, displayed: true)    
        unschedule(updateTimer)    
        // no error
        sendEvent(name: "errorCode", value: state.errorCode[0], display: true, displayed: true)            
    }    
}

def startTimer(timeInSeconds) {
    def newTime = timeInSeconds.toInteger()
    if (newTime < 0) {
        // new value sets timer below 0
        sendEvent(name: "errorCode", value: state.errorCode[4], display: true, displayed: true)
    } else if (newTime > maxTimerLimitSec.toInteger()) {
        // new value exceeds max time     
        sendEvent(name: "errorCode", value: state.errorCode[3], display: true, displayed: true)
    } else {
        log.info "startTimer: ${newTime}"     
        // set timer status to running and update time 
        sendEvent(name: "timeRemaining", value: newTime, display: true, displayed: true)
        state.timeRemaining = newTime   
        // start the timer
        start() 
    }
}

def setTimeRemaining(timeInSeconds) {
    def newTime = state.timeRemaining + timeInSeconds.toInteger()
    
    if (isTimerIdle()) {
        // no timer running
        sendEvent(name: "errorCode", value: state.errorCode[1], display: true, displayed: true)
    } else if (newTime >= maxTimerLimitSec.toInteger()) {
        // new value exceeds max time
        sendEvent(name: "errorCode", value: state.errorCode[3], display: true, displayed: true)
    } else if (newTime < 0) {
        // new value sets timer below 0
        sendEvent(name: "errorCode", value: state.errorCode[4], display: true, displayed: true)
    } else {
        log.info "setTimeRemaining: ${newTime}"     
        // set timer status to running and update time  
        sendEvent(name: "timeRemaining", value: newTime, display: true, displayed: true)    
        state.timeRemaining = newTime
        // no error
        sendEvent(name: "errorCode", value: state.errorCode[0], display: true, displayed: true)        
    }
}

def start() {
    if (isTimerIdle()) {
        // no timer running
        sendEvent(name: "errorCode", value: state.errorCode[1], display: true, displayed: true)
    } else {    
        log.info "start"       
        sendEvent(name: "sessionStatus", value: state.sessionStatus[2], display: true, displayed: true)  
        // no error
        sendEvent(name: "errorCode", value: state.errorCode[0], display: true, displayed: true)      
        // start the timer
        runIn(1,updateTimer)         
    }
}

def stop() {
    log.info "stop"    
    sendEvent(name: "sessionStatus", value: state.sessionStatus[0], display: true, displayed: true)    
    resetTimer() 
}

def resetTimer() {
    state.timeRemaining = -1    
    sendEvent(name: "timeRemaining", value: state.timeRemaining, display: true, displayed: true)   
    unschedule(updateTimer)
    // no error
    sendEvent(name: "errorCode", value: state.errorCode[0], display: true, displayed: true)        
}

def isTimerIdle() {
    if (state.timeRemaining == -1) {
        return true
    } else {
        return false
    }
}   

def updateTimer() {
    if (--state.timeRemaining == 0) {
        stop()
    } else {
        // update the time remaining
        sendEvent(name: "timeRemaining", value: state.timeRemaining, display: true, displayed: true)
        runIn(1,updateTimer)      
    }
}
