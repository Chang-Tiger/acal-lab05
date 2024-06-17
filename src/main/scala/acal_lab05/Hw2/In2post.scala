package acal_lab05.Hw2
import chisel3._
import chisel3.util._


class CpxCal_In2post(keyin_size : Int =32) extends Module{
    val io = IO(new Bundle{
        val store_in = Input(UInt(keyin_size.W))
        val store_in_ifOperand = Input(Bool()) //true:opeand /false:symbol
        val input1_valid = Input(Bool())
        val in2post_dataOut = Output(Valid(UInt(keyin_size.W)))
        val if_operand_in2post_dataOut = Output(Valid(Bool()))  //1:operand, 0:operator
    }) 
    
    io.in2post_dataOut.bits := 0.U
    io.in2post_dataOut.valid := false.B
    io.if_operand_in2post_dataOut.bits := false.B
    io.if_operand_in2post_dataOut.valid := false.B

    val in2post_rfile = Module(new In2post_rfile(100)) //store incoming stream(like quene)
    in2post_rfile.io.dataIn := io.store_in
    in2post_rfile.io.if_operand := io.store_in_ifOperand
    in2post_rfile.io.push := io.input1_valid 
    in2post_rfile.io.pop := false.B 
    in2post_rfile.io.en := true.B
    in2post_rfile.io.reset := false.B

    val incoming_data = WireDefault(0.U(keyin_size.W))
    val incoming_if = WireDefault(false.B)
    incoming_data := in2post_rfile.io.dataOut 
    incoming_if :=  in2post_rfile.io.if_operand_dataOut


    val sym_st = Module(new Stack(100,32)) // stack of storing symble 
    sym_st.io.push := false.B
    sym_st.io.pop := false.B
    sym_st.io.en := true.B
    sym_st.io.dataIn := incoming_data  
    val stack_out = WireDefault(0.U(keyin_size.W))
    stack_out := sym_st.io.dataOut
    val stack_empty_buffer = RegNext(sym_st.io.empty)



    


    val out_value = RegInit(0.U(keyin_size.W))
    val out_if = RegInit(false.B)
    val out_valid = RegInit(false.B)
    out_value := 0.U
    out_if := false.B
    out_valid := false.B

    val if_pop = RegInit(false.B)
    val if_pop1 = RegInit(false.B)
    val if_push = RegInit(false.B)
    val if_right_pop =RegInit(false.B)

    val if_left = WireDefault(false.B)
    if_left := incoming_data  === 13.U &&  (!incoming_if) 
    val if_right = WireDefault(false.B)
    if_right := (incoming_data === 14.U  &&  (!incoming_if) )
    val if_equal = WireDefault(false.B)
    if_equal := ( incoming_data === 15.U  &&  (!incoming_if) )

    val if_one_state = RegInit(false.B)
    val if_equal_state = RegInit(false.B)
    val if_symble_state = RegInit(false.B)
    val if_left_state = RegInit(false.B)
    val if_left_stackout_state = RegInit(false.B)
    val equal_ifpop = RegInit(false.B)
    val count = RegInit(0.U(3.W))
    val count1 = RegInit(0.U(3.W))

    
    val precedence =RegInit(3.U(2.W)) //define precedence
    precedence := Mux( incoming_data <= 12.U  && incoming_data >= 10.U,  
        Mux( incoming_data =/= 0.U && !incoming_if && (( (incoming_data === 10.U) && stack_out === 12.U) || (incoming_data === 11.U && stack_out === 12.U))  , 0.U, //lower
        Mux(incoming_data =/= 0.U && !incoming_if && ((incoming_data === stack_out) || (incoming_data === 10.U && stack_out === 11.U) || (incoming_data === 11.U && stack_out === 10.U)), 1.U, //equal
        Mux(incoming_data =/= 0.U && !incoming_if && ((incoming_data === 12.U && stack_out === 10.U) || (incoming_data === 12.U && stack_out === 11.U)), 2.U ,3.U) ) ) , 3.U)


    when(!io.input1_valid){
        when(incoming_if && !if_one_state){ //if the operand coming, output immediately 
            if_equal_state := false.B
            if_symble_state := false.B
            if_left_state := false.B
            if_left_stackout_state := false.B

            when(!if_one_state){
                in2post_rfile.io.pop := true.B
                out_value := in2post_rfile.io.dataOut //incoming_data
                out_if := in2post_rfile.io.if_operand_dataOut //incoming_if
                out_valid := 1.U
                if_one_state := true.B
            }.otherwise{
                out_value := 0.U 
                out_if := 0.U 
                out_valid := false.B

            }
            
        }.elsewhen(incoming_data =/= 0.U && !incoming_if){
            if_one_state := false.B
            equal_ifpop := !equal_ifpop
            count1 := Mux(count1 === 5.U, 0.U, count1 + 1.U )
            if_pop1 := Mux(count1 === 5.U, false.B, if_pop1 )
            if_symble_state := Mux(count1 === 5.U, false.B, if_symble_state )

            when(if_equal) {
                when(!stack_empty_buffer ){
                    sym_st.io.pop := Mux(!equal_ifpop, true.B, false.B) 
                    out_value := stack_out
                    out_if := 0.U
                    out_valid := Mux(!equal_ifpop, true.B, false.B)
                }.elsewhen(!if_equal_state){
                    in2post_rfile.io.pop := true.B
                    out_value := 15.U
                    out_if := 0.U
                    out_valid := 1.U
                    if_equal_state := true.B
                }.otherwise{
                out_value := 0.U //incoming_data
                out_if := 0.U //incoming_if
                out_valid := false.B

                }
                
            }.elsewhen( if_right  ){
                when( !if_right_pop ){
                    sym_st.io.pop := true.B
                    out_value := stack_out
                    out_if := 0.U
                    out_valid := Mux(stack_out === 13.U, false.B, true.B) 
                    in2post_rfile.io.pop := Mux(stack_out === 13.U, true.B, false.B)
                }.otherwise{
                    out_value := 0.U 
                    out_if := 0.U 
                    out_valid := false.B
                }
                if_right_pop := !if_right_pop
            }.elsewhen( (precedence === 1.U && !if_pop) || (precedence === 0.U  && !if_pop1)){ // sym_stack pop
                when(!if_symble_state){
                sym_st.io.pop := true.B
                out_value := stack_out
                out_if := 0.U
                out_valid := true.B
                if_pop := Mux(precedence === 1.U , true.B, if_pop)  
                if_pop1 := Mux(precedence === 0.U , true.B, if_pop1) 
                if_push := false.B
                if_symble_state := true.B
                count1 := 0.U}
            }.elsewhen( (precedence === 1.U && if_pop && !if_push) || precedence === 2.U){ //sym_stack push
                when(!if_symble_state){
                    in2post_rfile.io.pop := true.B
                    sym_st.io.push := true.B
                    sym_st.io.dataIn := incoming_data
                    if_pop := false.B
                    if_push :=Mux(precedence === 2.U, false.B, true.B) 
                    if_symble_state := true.B
                    count1 := 0.U
                    
                }
                out_value := 0.U 
                out_if := 0.U 
                out_valid := false.B
                
            }.elsewhen(( if_left ) ){ //left bracket

                out_value := 0.U 
                out_if := 0.U 
                out_valid := false.B
                if_one_state := false.B
                count := Mux(count === 5.U, 0.U, count + 1.U)
                if_left_state := Mux(count === 5.U, false.B, if_left_state)
                when(!if_left_state && count === 5.U){
                    in2post_rfile.io.pop := true.B   
                    sym_st.io.push := true.B
                    sym_st.io.dataIn := in2post_rfile.io.dataOut
                    if_equal_state := false.B 
                    if_symble_state := false.B
                    if_pop := false.B
                    if_pop1 :=false.B
                    if_left_state := !if_left_state //!
                    }            
            }.elsewhen(sym_st.io.empty ){

            out_value := 0.U 
            out_if := 0.U 
            out_valid := false.B
            if_one_state := false.B   
            in2post_rfile.io.pop := true.B   
            sym_st.io.push := true.B
            sym_st.io.dataIn := in2post_rfile.io.dataOut
            if_equal_state := false.B          
            if_pop := false.B

            }.elsewhen(stack_out === 13.U){
                out_value := 0.U 
                out_if := 0.U 
                out_valid := false.B
                
                when(!if_left_stackout_state){ //deal the delay of stack's pop
                    in2post_rfile.io.pop := true.B   
                    sym_st.io.push := true.B
                    sym_st.io.dataIn := in2post_rfile.io.dataOut
                    if_equal_state := false.B 
                    if_symble_state := false.B
                    if_pop := false.B
                    if_left_stackout_state :=  !if_left_stackout_state 
                }
            }    
        }.otherwise{
                out_value := 0.U //incoming_data
                out_if := 0.U //incoming_if
                out_valid := false.B

        }  
    }     
        io.in2post_dataOut.bits := out_value
        io.in2post_dataOut.valid := out_valid
        io.if_operand_in2post_dataOut.bits := out_if 
        io.if_operand_in2post_dataOut.valid := out_valid


    

}