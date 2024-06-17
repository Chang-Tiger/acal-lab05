package acal_lab05.Hw2

import chisel3._
import chisel3.util._

class NegIntGen extends Module{
    val io = IO(new Bundle{
        val key_in = Input(UInt(4.W))
        val value = Output(Valid(UInt(32.W)))
    })

    val equal = WireDefault(false.B)
    equal := io.key_in === 15.U
    
    val sIdle :: sAccept :: sEqual ::Nil = Enum(3)
    val state = RegInit(sIdle)
    //Next State Decoder
    switch(state){
        is(sIdle){
        state := sAccept
        }
        is(sAccept){
        when(equal) {state := sEqual}
        }
        is(sEqual){
            state := sAccept
        }
    }

    val in_buffer = RegNext(io.key_in)
    val neg =  RegInit(false.B)
    val number = RegInit(0.U(32.W))
    when(state === sAccept){
        when(in_buffer === 11.U){
            neg:=true.B
            number := number
        }.elsewhen(in_buffer === 13.U || in_buffer === 14.U){
            number := number
        }.otherwise{
            number := (number<<3.U) + (number<<1.U) + in_buffer
        }
    }.elsewhen(state === sEqual){
        neg := false.B
        in_buffer := 0.U
        number := 0.U
    }

    io.value.valid := Mux(state === sEqual,true.B,false.B)
    io.value.bits := Mux(neg,(number.asSInt * (-1.S)).asUInt,number)
    
}