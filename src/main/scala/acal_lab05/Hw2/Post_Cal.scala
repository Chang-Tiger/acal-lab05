package acal_lab05.Hw2

import chisel3._
import chisel3.util._


class Post_Cal extends Module{
    val io = IO(new Bundle{
        val input_data = Input(UInt(32.W))
        val input_data_isnum = Input(Bool())
        val input_data_valid  = Input(Bool())
        val results = Output(Valid(UInt(32.W)))
    }) 
    
    io.results.bits := 0.U
    io.results.valid := false.B


    
    val buffer_que = Module(new Queue_buffer(32))
    buffer_que.io.dataIn := io.input_data
    buffer_que.io.is_num := io.input_data_isnum
    buffer_que.io.push := io.input_data_valid 
    buffer_que.io.pop := false.B 
    buffer_que.io.en := true.B
    buffer_que.io.reset := false.B

    val incoming_data = WireDefault(0.U(32.W))
    val incoming_isnum = Wire(Bool())
    incoming_data := buffer_que.io.dataOut 
    incoming_isnum :=  buffer_que.io.is_num_dataOut
    val isequal = Wire(Bool())
    isequal := incoming_data === 15.U && !incoming_isnum

    val pcal_stack = Module(new Stack(32))
    pcal_stack.io.push := false.B
    pcal_stack.io.pop := false.B
    pcal_stack.io.en := true.B
    pcal_stack.io.dataIn := incoming_data  
    val stack_top = WireDefault(0.U(32.W))
    stack_top := pcal_stack.io.dataOut
    //val stack_empty_buffer = RegNext(pcal_stack.io.empty)

    

    val output_complete = RegInit(false.B)
    val if_pop = RegInit(false.B)
    val counter = RegInit(true.B)
    
    val num0 = RegInit(0.U(32.W))
    val num1 = RegInit(0.U(32.W))
    val add = 0.U
    val sub = 1.U
    val mul = 2.U 


    when(!io.input_data_valid ){
        counter := !counter//push一次換正負讓queue有值
        when(counter){
            when(incoming_isnum){//number push in
                buffer_que.io.pop := true.B
                pcal_stack.io.push := true.B
                pcal_stack.io.pop := false.B
                io.results.valid  := false.B
                output_complete := false.B
                
            }.elsewhen(incoming_data =/= 0.U && !incoming_isnum){
                buffer_que.io.pop := false.B
                pcal_stack.io.push := false.B
                when(isequal){
                    when(output_complete){
                        io.results.bits := 0.U
                        io.results.valid  := false.B
                    }.elsewhen(!pcal_stack.io.empty){
                        pcal_stack.io.pop := true.B
                        pcal_stack.io.push := false.B
                        buffer_que.io.pop := true.B
                        
                        io.results.bits := stack_top
                        io.results.valid := true.B
                        output_complete := true.B
                    }

                }.otherwise{  //其他運算+,-,*
                    when(!pcal_stack.io.empty && num0 === 0.U){
                        when(!if_pop){//pop一個出來
                            pcal_stack.io.pop := true.B
                            num0 := stack_top
                            if_pop := true.B
                        }
                    }.elsewhen(num1 === 0.U){
                        when(if_pop){//再pop一個出來運算
                            pcal_stack.io.pop := Mux( num0 =/= 0.U && !pcal_stack.io.empty, true.B, false.B)
                            num1 := Mux(pcal_stack.io.empty, 0.U, stack_top ) 
                            if_pop := false.B
                        }
                    }.otherwise{
                        pcal_stack.io.pop := false.B
                        pcal_stack.io.push := true.B
                        buffer_que.io.pop := true.B
                        
                        when(incoming_data - 10.U === add){pcal_stack.io.dataIn := (num1 + num0)}
                        .elsewhen(incoming_data - 10.U === sub){pcal_stack.io.dataIn := (num1 - num0)}
                        .elsewhen(incoming_data - 10.U === mul){pcal_stack.io.dataIn := (num1 * num0)}
                        
                        num0 := 0.U
                        num1 := 0.U                   
                        if_pop := false.B
                    }
                }
            }
        } 
    }      
    
}