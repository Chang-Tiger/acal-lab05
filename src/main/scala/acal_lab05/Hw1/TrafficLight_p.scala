package acal_lab05.Hw1

import chisel3._
import chisel3.util._

class TrafficLight_p(Ytime:Int, Gtime:Int, Ptime:Int) extends Module{
  val io = IO(new Bundle{
    val P_button = Input(Bool())
    val H_traffic = Output(UInt(2.W))
    val V_traffic = Output(UInt(2.W))
    val P_traffic = Output(UInt(2.W))
    val timer     = Output(UInt(5.W))
  })
  
  //parameter declaration
  val Off = 0.U
  val Red = 1.U
  val Yellow = 2.U
  val Green = 3.U

  val sIdle :: sHGVR :: sHYVR :: sHRVG :: sHRVY :: sPG :: Nil = Enum(6)

  //State register
  val state = RegInit(sIdle)
  val save_state = RegInit(sIdle)

  //Counter============================
  val cntMode = WireDefault(0.U(1.W))
  val cntReg = RegInit(0.U(4.W))
  val cntDone = Wire(Bool())
  cntDone := cntReg === 0.U

  when(io.P_button){
    cntReg := (Ptime-1).U
  }.elsewhen(cntDone){
    when(cntMode === 0.U){
      cntReg := (Gtime-1).U
    }.elsewhen(cntMode === 1.U){
      cntReg := (Ytime-1).U
    }
  }.otherwise{
    cntReg := cntReg - 1.U
  }
  //Counter end========================

  //Next State Decoder
  switch(state){
    is(sIdle){
      state := sHGVR
    }
    is(sHGVR){
      when(io.P_button) {
        when(cntDone){
          save_state:=sHYVR 
        }.otherwise{
          save_state:=sHGVR
        }
        
        //cntReg := 0.U
        state := sPG
      }.otherwise{
        when(cntDone) {state := sHYVR}
      }
    }
    is(sHYVR){
      when(io.P_button) {
        when(cntDone){
          save_state:=sHRVG
        }.otherwise{
          save_state:=sHYVR
        }
        //cntReg := 0.U
        state := sPG
      }.otherwise{
        when(cntDone) {state := sHRVG}
      }
    }
    is(sHRVG){
      when(io.P_button) {
        when(cntDone){
          save_state:=sHRVY
        }.otherwise{
          save_state:=sHRVG
        }
        //cntReg := 0.U
        state := sPG
      }.otherwise{
        when(cntDone) {state := sHRVY}
      }
    }
    is(sHRVY){
      when(io.P_button) {
        when(cntDone){
          save_state:=sHGVR
        }.otherwise{
          save_state:=sHRVY
        }
        //cntReg := 0.U
          state := sPG
      }.otherwise{
        when(cntDone) {state := sHGVR}
      }
    }
    is(sPG){
      when(cntDone) {state := save_state}
    }
  }

  //Output Decoder
  //Default statement
  cntMode := 0.U
  io.H_traffic := Off
  io.V_traffic := Off
  io.P_traffic := Off

  switch(state){
    is(sHGVR){
      cntMode := 1.U
      io.H_traffic := Green
      io.V_traffic := Red
      io.P_traffic := Red
    }
    is(sHYVR){
      cntMode := 0.U
      io.H_traffic := Yellow
      io.V_traffic := Red
      io.P_traffic := Red
    }
    is(sHRVG){
      cntMode := 1.U
      io.H_traffic := Red
      io.V_traffic := Green
      io.P_traffic := Red
    }
    is(sHRVY){
      cntMode := 0.U
      io.H_traffic := Red
      io.V_traffic := Yellow
      io.P_traffic := Red
    }
    is(sPG){
      switch(save_state){
        is(sHGVR){
          cntMode := 0.U
        }
        is(sHRVG){
          cntMode := 0.U
        }
        is(sHYVR){
          cntMode := 1.U
        }
        is(sHRVY){
          cntMode := 1.U
        }
      }
      io.H_traffic := Red
      io.V_traffic := Red
      io.P_traffic := Green
    }
  }

  io.timer := cntReg

}