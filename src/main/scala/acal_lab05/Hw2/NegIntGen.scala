package acal_lab05.Hw2

import chisel3._
import chisel3.util._

class NegIntGen extends Module{
    val io = IO(new Bundle{
        val key_in = Input(UInt(4.W))
        val value = Output(Valid(UInt(32.W)))
    })
    
    //please implement your code below

    //Wire Declaration============
    val equal = WireDefault(false.B)
    equal := io.key_in === 15.U
    
    val num = WireDefault(false.B)
    num:= io.key_in <10.U

    val operator = WireDefault(false.B)
    operator := io.key_in >=10.U && io.key_in <= 14.U 
    
    //Reg Declaration
    val in_buffer = RegNext(io.key_in)
    val number = RegInit(0.U(32.W))  
    val op = RegInit(0.U(2.W))

    val if_left = RegInit(false.B) //when left bracket appear, if_left = true
    val if_neg = RegInit(false.B) //when minus appear ,if_neg = !(if neg)
    val left_counter = RegInit(0.U(4.W)) 
    


    val sIdle :: sAccept :: sOp :: sEqual :: Nil = Enum(4)
    val state = RegInit(sIdle)
    //Next State Decoder
    switch(state){
        is(sIdle){
            when(equal) {
                state := sEqual
            }.elsewhen(operator){
                state := sOp
            }.otherwise { state := sAccept}
        
        }
        is(sAccept){
            when(equal) {
                state := sEqual
            }.elsewhen(operator){
                state := sOp
            }
        }
        is(sOp){
            when(num){
                state := sAccept
            }.elsewhen(equal){
                state := sEqual
            }
        }
        is(sEqual){
            state := sIdle //sAccept
        }
    }

    

    when(state === sAccept){
        when(in_buffer < 10.U){
            number := (number<<3.U) + (number<<1.U) + in_buffer}
    }.elsewhen(state === sOp){
         //op= '('
        when(in_buffer === 13.U ){       
            if_left := 1.U
            left_counter := left_counter + 1.U
        }.elsewhen(in_buffer ===14.U){  //op= ')'
            // if left bracked has appeared, check if_neg = true,and let number become minus_number
            when(if_left ){
                when(if_neg){ number:= (~number).asUInt +1.U }
                left_counter := left_counter - 1.U
                if_left := Mux(left_counter === 0.U, 0.U , 1.U)
            }
        }.elsewhen(in_buffer === 11.U){    //op = '-'
            if_neg := !(if_neg)
        }
    }.elsewhen(state === sEqual){
        in_buffer := 0.U
        number := 0.U
        if_left := false.B
        left_counter := 0.U
        if_neg := false.B
    }

    io.value.valid := Mux(state === sEqual,true.B,false.B)
    io.value.bits := number

}