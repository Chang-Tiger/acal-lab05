package acal_lab05.Hw2

import chisel3._
import chisel3.util._

class CpxCal extends Module{
    val io = IO(new Bundle{
        val key_in = Input(UInt(4.W))
        val value = Output(Valid(UInt(32.W)))    
    })
    
    io.value.valid := false.B
    io.value.bits := 0.U


    val get_and_store = Module(new GetandStore())
    get_and_store.io.key_in := io.key_in

    val in2post = Module(new My_In2post())
    in2post.io.input_data := get_and_store.io.getData.bits
    in2post.io.input_data_valid := get_and_store.io.getData.valid
    in2post.io.input_data_isnum := get_and_store.io.is_num


    val post_cal= Module(new Post_Cal())
    post_cal.io.input_data := in2post.io.in2post_dataOut.bits 
    post_cal.io.input_data_isnum := in2post.io.is_num_out
    post_cal.io.input_data_valid := in2post.io.in2post_dataOut.valid
    

    io.value.valid := post_cal.io.results.valid
    io.value.bits := post_cal.io.results.bits

}

