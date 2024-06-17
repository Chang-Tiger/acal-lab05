package acal_lab05.Hw2
import chisel3._
import chisel3.util._



class Queue_buffer(val depth: Int) extends Module {
    val io = IO(new Bundle {
    val push    = Input(Bool())
    val pop     = Input(Bool())
    val en      = Input(Bool())
    val reset    = Input(Bool())
    val dataIn  = Input(UInt(32.W))
    val is_num = Input(Bool())
    val dataOut = Output(UInt(32.W))
    val is_num_dataOut = Output(Bool())
    val empty   = Output(Bool())
    val full    = Output(Bool())
    })


    val queue_mem = Mem(depth, UInt(32.W))
    val queue_isNum_mem = Mem(depth, Bool())//Mem(depth, UInt(32.W))
    val sp        = RegInit(0.U(log2Ceil(depth+1).W))
    val out       = RegInit(0.U(32.W))
    val isNum_out       = RegInit(false.B)


    when (io.en) {
            when(io.push && (sp < depth.asUInt)) {
            queue_mem(0) := io.dataIn
            queue_isNum_mem(0) := io.is_num
        for( i <- 0 to (depth-2)){
            queue_mem(i+1) := queue_mem(i)
            queue_isNum_mem(i+1) := queue_isNum_mem(i)
        }
            sp := sp + 1.U
        } .elsewhen(io.pop && (sp > 0.U)) {
            sp := sp - 1.U
        }
        when (sp > 0.U) {
            out := queue_mem(sp - 1.U)
            isNum_out := queue_isNum_mem(sp - 1.U)
        }.otherwise{
            out := 0.U
            isNum_out := 0.U
        }
    }
    when(io.reset){
        out := 0.U
        isNum_out := 0.U
        sp := 0.U
    }
    io.empty := Mux(sp === 0.U, true.B, false.B)
    io.full:= Mux(sp === depth.asUInt , true.B, false.B)
    io.dataOut := out
    io.is_num_dataOut := isNum_out
}


class My_In2post(keyin_size : Int =200) extends Module{
    val io = IO(new Bundle{
        val input_data = Input(UInt(keyin_size.W))
        val input_data_isnum = Input(Bool())
        val input_data_valid = Input(Bool())
        val in2post_dataOut = Output(Valid(UInt(keyin_size.W)))
        val is_num_out = Output(Bool())
    }) 
    
    io.in2post_dataOut.bits := 0.U
    io.in2post_dataOut.valid := false.B
    io.is_num_out := false.B
    

    val in2post_queue = Module(new Queue_buffer(200))
    in2post_queue.io.dataIn := io.input_data
    in2post_queue.io.is_num := io.input_data_isnum

    in2post_queue.io.push := io.input_data_valid 
    in2post_queue.io.pop := false.B 
    in2post_queue.io.en := true.B
    in2post_queue.io.reset := false.B

    val incoming_data = WireDefault(0.U(keyin_size.W))
    val incoming_isnum = Wire(Bool())
    incoming_data := in2post_queue.io.dataOut 
    incoming_isnum :=  in2post_queue.io.is_num_dataOut

    val in2post_stack = Module(new Stack(200))
    in2post_stack.io.push := false.B
    in2post_stack.io.pop := false.B
    in2post_stack.io.en := true.B
    in2post_stack.io.dataIn := incoming_data  
    val stack_top = WireDefault(0.U(keyin_size.W))
    stack_top := in2post_stack.io.dataOut
    val stack_empty_buffer = RegNext(in2post_stack.io.empty)



    val out_value = RegInit(0.U(keyin_size.W))
    val out_isnum = RegInit(false.B)
    val out_valid = RegInit(false.B)
    out_value := 0.U
    out_isnum := false.B
    out_valid := false.B

    val if_pop_ = RegInit(false.B)
    val isright_pop =RegInit(false.B)

    val isleft = Wire(Bool())
    isleft := incoming_data  === 13.U &&  (!incoming_isnum) 
    val isright = Wire(Bool())
    isright := incoming_data === 14.U  &&  (!incoming_isnum)
    val isequal = Wire(Bool())
    isequal := incoming_data === 15.U  &&  (!incoming_isnum)

    val in_one_state = RegInit(false.B)
    val is_equal_state = RegInit(false.B)
    val is_symbol_state = RegInit(false.B)

    val is_left_state = RegInit(false.B)
    val eq_output = RegInit(false.B)
    val counter0 = RegInit(0.U(3.W))

    val wait_time = 2.U
    
    val judge_result =RegInit(2.U(2.W)) //define judge_result
    
    when(incoming_data < 13.U  && incoming_data > 9.U && !incoming_isnum && stack_top < 13.U && stack_top > 9.U){
        when(incoming_data <= stack_top){judge_result := 0.U}//priority <=stack top時
        .elsewhen(incoming_data > stack_top){judge_result := 1.U}//priority >stack top時
    }.otherwise{judge_result := 2.U}
 
  

    when(!io.input_data_valid){//等到不是input data狀況操作，一個number可能持續多個cycle，只需要output一次
        when(incoming_isnum && !in_one_state){ //number output directly
            is_equal_state := false.B
            is_symbol_state := false.B
            is_left_state := false.B
            in2post_queue.io.pop := true.B
            out_value := in2post_queue.io.dataOut
            out_isnum := in2post_queue.io.is_num_dataOut
            out_valid := true.B
            in_one_state := true.B
            
            
        }.elsewhen(!incoming_isnum && incoming_data =/= 0.U){
            in_one_state := false.B
            eq_output := !eq_output
            counter0 := Mux(counter0 === wait_time, 0.U, counter0 + 1.U )//用wait time中斷 不能一直連續pop queue，否則會導致錯誤
            if_pop_ := Mux(counter0 === wait_time, false.B, if_pop_ )
            is_symbol_state := Mux(counter0 === wait_time, false.B, is_symbol_state )

            when(isequal) {//incoming是equal pop剩下stack
                when(!stack_empty_buffer ){
                    in2post_stack.io.pop := !eq_output
                    out_value := stack_top
                    out_isnum := false.B
                    out_valid := !eq_output
                }.elsewhen(!is_equal_state){//output equal
                    in2post_queue.io.pop := true.B
                    out_value := 15.U
                    out_isnum := false.B
                    out_valid := true.B
                    is_equal_state := true.B
                }.otherwise{
                    out_value := 0.U
                    out_isnum := false.B
                    out_valid := false.B
                }
                
            }.elsewhen( isright){//incoming是right bracket，pop stack直到pop出left bracket
                when( !isright_pop ){
                    in2post_stack.io.pop := true.B
                    out_value := stack_top
                    out_isnum := false.B
                    out_valid := !(stack_top === 13.U)
                    in2post_queue.io.pop := (stack_top === 13.U)
                }.otherwise{
                    out_value := 0.U 
                    out_isnum := false.B
                    out_valid := false.B
                }
                isright_pop := !isright_pop
            }.elsewhen( (judge_result === 0.U  && !if_pop_)){ // incoming優先級<=stack，stack pop並output
                when(!is_symbol_state){
                    in2post_stack.io.pop := true.B
                    out_value := stack_top
                    out_isnum := false.B
                    out_valid := true.B
                    if_pop_ := true.B
                    is_symbol_state := true.B
                    counter0 := 0.U
                }
            }.elsewhen( judge_result === 1.U || stack_top === 13.U){ //優先級大於stack-> push
                when(!is_symbol_state){
                    in2post_queue.io.pop := true.B
                    in2post_stack.io.push := true.B
                    is_symbol_state := true.B
                    counter0 := 0.U     
                }
                out_value := 0.U 
                out_isnum := false.B 
                out_valid := false.B
                
            }.elsewhen(isleft){ //incoming 是left bracket，直接push進入stack

                out_value := 0.U 
                out_isnum := false.B  
                out_valid := false.B
                
                 when(!is_left_state){
                    is_equal_state := false.B 
                    is_symbol_state := false.B
                    in2post_queue.io.pop := true.B   
                    in2post_stack.io.push := true.B
                    if_pop_ :=false.B
                    is_left_state := !is_left_state
                 }
                           
            }.elsewhen(in2post_stack.io.empty ){
                out_value := 0.U 
                out_isnum := false.B
                out_valid := false.B
                is_equal_state := false.B  
                in2post_queue.io.pop := true.B   
                in2post_stack.io.push := true.B
            }    
        }.otherwise{
            out_value := 0.U
            out_isnum := false.B
            out_valid := false.B
        }  
    }     
    io.in2post_dataOut.bits := out_value
    io.in2post_dataOut.valid := out_valid
    io.is_num_out := out_isnum 
}
