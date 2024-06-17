package acal_lab05.Hw2

import chisel3._
import chisel3.util._

class CpxCal(count_bit : Int = 150) extends Module{
    val io = IO(new Bundle{
        val key_in = Input(UInt(4.W))
        val value = Output(Valid(UInt(count_bit.W)))    
    })

    //please implement your code below
    io.value.valid := false.B
    io.value.bits := 0.U

    val in_buffer = RegNext(io.key_in)

    val store1 = Module(new CpxCal_Store(4))
    store1.io.key_in := in_buffer //store1.io.key_in := 0.U(4.W)

    val in2post = Module(new CpxCal_In2post(count_bit))
    in2post.io.store_in := store1.io.store_dataOut.bits
    in2post.io.store_in_ifOperand := store1.io.if_operand_dataOut.bits
    in2post.io.input1_valid := store1.io.store_dataOut.valid

    val calculate= Module(new CpxCal_Calculate(count_bit))
    calculate.io.store_in := in2post.io.in2post_dataOut.bits
    calculate.io.store_in_ifOperand := in2post.io.if_operand_in2post_dataOut.bits //true:opeand /false:symbol
    calculate.io.input1_valid := in2post.io.in2post_dataOut.valid

    val cal_out = RegInit(0.U(count_bit.W))
    val cal_out_valid  = RegInit(false.B)    
    cal_out := calculate.io.cal_dataOut.bits
    cal_out_valid  := calculate.io.cal_dataOut.valid   



    io.value.valid := cal_out_valid
    io.value.bits := cal_out
    
    
    
    
}

