package acal_lab05.Hw2

import chisel3._
import chisel3.util._


class CpxCal_Calculate(keyin_size : Int =32, count_bit : Int = 150) extends Module{
    val io = IO(new Bundle{
        val store_in = Input(UInt(keyin_size.W))
        val store_in_ifOperand = Input(Bool()) //true:opeand /false:symbol
        val input1_valid = Input(Bool())
        val cal_dataOut = Output(Valid(UInt(count_bit .W)))
    }) 
    
    io.cal_dataOut.bits := 0.U
    io.cal_dataOut.valid := false.B


    val cal_rfile = Module(new Cal_rfile(100))
    cal_rfile.io.dataIn := io.store_in
    cal_rfile.io.if_operand := io.store_in_ifOperand
    cal_rfile.io.push := io.input1_valid 
    cal_rfile.io.pop := false.B 
    cal_rfile.io.en := true.B
    cal_rfile.io.reset := false.B


    val incoming_data = WireDefault(0.U(count_bit.W))
    val incoming_if = WireDefault(false.B)
    incoming_data := cal_rfile.io.dataOut 
    incoming_if :=  cal_rfile.io.if_operand_dataOut
    val if_equal = WireDefault(false.B)
    if_equal := (incoming_data === 15.U && !incoming_if)

    val cal_st = Module(new Stack(100))
    cal_st.io.push := false.B
    cal_st.io.pop := false.B
    cal_st.io.en := true.B
    cal_st.io.dataIn := incoming_data  
    //val stack_out = RegInit(0.U(32.W))
    val stack_out = WireDefault(0.U(count_bit.W))
    stack_out := cal_st.io.dataOut
    //val stack_empty = RegInit(false.B)
    val stack_empty_buffer = RegNext(cal_st.io.empty)

    val out_value = RegInit(0.U(count_bit.W))
    val out_if = RegInit(false.B)
    val out_valid = RegInit(false.B)
    out_value := 0.U
    out_if := false.B
    out_valid := false.B

    val if_out = RegInit(false.B)
    val if_pop = RegInit(false.B)
    val counter = RegInit(true.B)
    
    val src1 = RegInit(0.U(count_bit.W))
    val src2 = RegInit(0.U(count_bit.W))
    val add = 0.U
    val sub = 1.U
    val mul = 2.U 


    when(!io.input1_valid){
        counter := !counter
        when(incoming_if && counter){
            cal_rfile.io.pop := true.B
            cal_st.io.push := true.B
            cal_st.io.pop := false.B
            out_valid := false.B
            if_out := false.B
            
        }.elsewhen(incoming_data =/= 0.U && !incoming_if && counter){
            cal_rfile.io.pop := false.B
            cal_st.io.push := false.B
            when(if_equal){
                
                when(if_out){
                    out_value := 0.U
                    out_valid := false.B
                }.elsewhen(!cal_st.io.empty){
                    cal_rfile.io.pop := true.B
                    cal_st.io.pop := true.B
                    cal_st.io.push := false.B
                    out_value := stack_out
                    out_valid := true.B
                    if_out := true.B
                }

            }.otherwise{  
                when(!cal_st.io.empty && src2 === 0.U){
                    when(!if_pop){
                        cal_st.io.pop := true.B
                        src2 := stack_out
                        if_pop := true.B

                    }
                    
                }.elsewhen( src1 === 0.U){
                    when(if_pop){
                    cal_st.io.pop := Mux( src2 =/= 0.U && !cal_st.io.empty, true.B, false.B)
                    src1 := Mux(cal_st.io.empty, 0.U, stack_out ) 
                    if_pop := false.B
                    }
                }.otherwise{
                    cal_st.io.pop := false.B
                    cal_rfile.io.pop := true.B
                    cal_st.io.push := true.B
                    cal_st.io.dataIn := MuxLookup(incoming_data - 10.U, 0.U, Seq(
                        add -> (src1 + src2),
                        sub -> (src1 - src2),
                        mul -> (src1 * src2)
                    ) )
                    src1 := 0.U
                    src2 := 0.U
                    if_pop := false.B
                }
            }

    }
        
    }      
        io.cal_dataOut.bits := out_value
        io.cal_dataOut.valid := out_valid



    


}