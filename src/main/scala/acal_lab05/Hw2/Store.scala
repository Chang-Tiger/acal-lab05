package acal_lab05.Hw2

import chisel3._
import chisel3.util._


class CpxCal_Store(keyin_size : Int =4, val count_bit : Int =150 ) extends Module{
    val io = IO(new Bundle{
        val key_in = Input(UInt(keyin_size.W))
        val store_dataOut = Output(Valid(UInt(count_bit.W)))
        val if_operand_dataOut = Output(Valid(Bool()))  //1:operand, 0:operator
        
    }) 

    io.store_dataOut.valid := false.B
    io.if_operand_dataOut.valid := false.B
    io.store_dataOut.bits := 0.U       
    io.if_operand_dataOut.bits := 0.U  

    
    val num = WireDefault(false.B)
    num:= io.key_in < 10.U

    val operator = WireDefault(false.B)
    operator := io.key_in >=10.U && io.key_in <= 12.U 

    val left_bracket = WireDefault(false.B)
    left_bracket := io.key_in === 13.U

    val right_bracket = WireDefault(false.B)
    right_bracket := io.key_in === 14.U

    val equal = WireDefault(false.B)
    equal := io.key_in === 15.U

    val in_buffer = RegNext(io.key_in)
    val in_buffer2 = RegNext(in_buffer)
    val src1 = RegInit(0.U(count_bit.W))
    val src2 = RegInit(0.U(count_bit.W))

    val if_neg = RegInit(false.B) //when minus appear ,if_neg = !(if neg)


    //State  Declaration========
    val sIdle :: sSrc1  :: sOp :: sLeft :: sSrc2 :: sRight :: sEqual   :: Nil = Enum(7)
    val state = RegInit(sIdle)
    val prestate = RegNext(state)

  //Next State Decoder
    
    switch(state){
        is(sIdle){
                when(operator) {state := sLeft}              
                .elsewhen(num){ state := sSrc1}
                .elsewhen(left_bracket){ state := sLeft}
        }
        is(sSrc1){
            when(operator) {state := sOp}
            .elsewhen(equal){state := sEqual}
        }
        is(sOp){
            when(num) {
                state := sSrc2
            }
            .elsewhen(equal){
                state := sEqual
            }
            .elsewhen(left_bracket){
                state := sLeft
            }
            
        }
        is(sLeft){
            when(num){
                state := sSrc2
            }
        }
        is(sSrc2){
            when(operator){ 
                state := sOp
            }
            .elsewhen(right_bracket){
                state := sRight
            }
            .elsewhen(equal){ 
                state := sEqual
            }
        }
        is(sRight){
            when(operator){
                state:= sOp
            }.elsewhen(equal){
                state := sEqual
            }
        }
        is(sEqual){
            (state:=sIdle)        
        }
    }

    when(state ===  sIdle){
        when(prestate === sEqual){
            io.store_dataOut.bits := 15.U
            io.store_dataOut.valid := 1.U
            io.if_operand_dataOut.bits := 0.U
            io.if_operand_dataOut.valid := 1.U
        }
    }
    
    when(state === sSrc1){
        io.store_dataOut.valid := false.B
        io.if_operand_dataOut.valid := false.B
        src1 := (src1<<3.U) + (src1<<1.U) + in_buffer

        when(prestate === sEqual){
            io.store_dataOut.bits := 15.U
            io.store_dataOut.valid := 1.U
            io.if_operand_dataOut.bits := 0.U
            io.if_operand_dataOut.valid := 1.U

        }
        
        }

    when(state === sOp){
        when(prestate === sSrc1 ){
            io.store_dataOut.bits := src1  
            io.store_dataOut.valid := 1.U 
            io.if_operand_dataOut.bits := 1.U
            io.if_operand_dataOut.valid := 1.U



        }.elsewhen( prestate === sSrc2){
            io.store_dataOut.bits := src2 
            io.store_dataOut.valid := 1.U 
            io.if_operand_dataOut.bits := 1.U
            io.if_operand_dataOut.valid := 1.U


        }.elsewhen( prestate === sRight){
            if_neg := Mux(if_neg,  !if_neg, if_neg)
            io.store_dataOut.bits := 14.U  
            io.store_dataOut.valid := !if_neg 
            io.if_operand_dataOut.bits := 0.U
            io.if_operand_dataOut.valid := !if_neg

        }.elsewhen(prestate === sEqual){
            io.store_dataOut.bits := 15.U
            io.store_dataOut.valid := 1.U
            io.if_operand_dataOut.bits := 0.U
            io.if_operand_dataOut.valid := 1.U

        }

    }
    when(state === sLeft){
        //if next left_bracket is minus, 
        when(in_buffer === 11.U){
            if_neg := !if_neg

        }.elsewhen(prestate === sOp){

            io.store_dataOut.bits := in_buffer2  
            io.store_dataOut.valid := 1.U 
            io.if_operand_dataOut.bits := 0.U
            io.if_operand_dataOut.valid := 1.U


        }.elsewhen(prestate === sLeft){

            io.store_dataOut.bits := 13.U  
            io.store_dataOut.valid := 1.U 
            io.if_operand_dataOut.bits := 0.U
            io.if_operand_dataOut.valid := 1.U


        }

        when(prestate === sEqual){
            io.store_dataOut.bits := 15.U
            io.store_dataOut.valid := 1.U
            io.if_operand_dataOut.bits := 0.U
            io.if_operand_dataOut.valid := 1.U

        }
    }
    when(state === sSrc2){
        when(prestate === sSrc2){
            src2 := (src2<<3.U) + (src2<<1.U) + in_buffer
        }.otherwise{
            src2 :=  in_buffer
        }
        when(prestate === sOp){
            io.store_dataOut.bits := in_buffer2  
            io.store_dataOut.valid := 1.U 
            io.if_operand_dataOut.bits := 0.U
            io.if_operand_dataOut.valid := 1.U


        }.elsewhen(prestate === sLeft){

        io.store_dataOut.bits := 13.U 
        io.store_dataOut.valid := !if_neg
        io.if_operand_dataOut.bits := 0.U
        io.if_operand_dataOut.valid := !if_neg


        }.elsewhen(prestate === sRight){ 
            if_neg := !if_neg 

            io.store_dataOut.bits := 14.U 
            io.store_dataOut.valid :=  !if_neg 
            io.if_operand_dataOut.bits := 0.U
            io.if_operand_dataOut.valid := !if_neg

        }
    }
    when(state === sRight){

        io.store_dataOut.bits := Mux(prestate === sRight, 14.U, Mux(if_neg, (~src2).asUInt + 1.U, src2 ) )  
        io.store_dataOut.valid := 1.U 
        io.if_operand_dataOut.bits := Mux(prestate === sRight, 0.U, 1.U )
        io.if_operand_dataOut.valid := 1.U


    }
    when(state === sEqual){

        io.store_dataOut.bits := MuxLookup(prestate , 0.U, Seq(
            sOp -> in_buffer2,
            sLeft -> 13.U,
            sSrc1 -> src1,
            sSrc2 -> src2,
            sRight -> 14.U
        ))  
        io.store_dataOut.valid := Mux(prestate === sRight, !if_neg, 1.U )
        io.if_operand_dataOut.bits := Mux(prestate === sSrc1 || prestate === sSrc2 , 1.U, 0.U)
        io.if_operand_dataOut.valid := Mux(prestate === sRight, !if_neg, 1.U )

        //when(prestate === sOp){
        //    io.store_dataOut.bits := in_buffer2  
        //    io.store_dataOut.valid := 1.U 
        //    io.if_operand_dataOut.bits := 0.U
        //    io.if_operand_dataOut.valid := 1.U
//
        //}
        //.elsewhen(prestate === sLeft){
        //    io.store_dataOut.bits := 13.U  
        //    io.store_dataOut.valid := 1.U 
        //    io.if_operand_dataOut.bits := 0.U
        //    io.if_operand_dataOut.valid := 1.U
//
        //}.elsewhen(prestate === sSrc1 ){
        //    io.store_dataOut.bits := src1  
        //    io.store_dataOut.valid := 1.U 
        //    io.if_operand_dataOut.bits := 1.U
        //    io.if_operand_dataOut.valid := 1.U
//
        //}.elsewhen( prestate === sSrc2){
//
        //    io.store_dataOut.bits := src2 
        //    io.store_dataOut.valid := 1.U 
        //    io.if_operand_dataOut.bits := 1.U
        //    io.if_operand_dataOut.valid := 1.U
//
//
        //}.elsewhen( prestate === sRight){
//
        //    io.store_dataOut.bits := 14.U
        //    io.store_dataOut.valid := !if_neg
        //    io.if_operand_dataOut.bits := 0.U
        //    io.if_operand_dataOut.valid := !if_neg
//
//
        //}

        when(prestate =/= sEqual){
            in_buffer := 0.U
            in_buffer2 := 0.U
            src1 := 0.U
            src2 := 0.U
            if_neg := false.B 
        

        }
        
    
    }
    

    


    

    

}