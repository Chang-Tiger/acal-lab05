// See LICENSE.txt for license details.
package acal_lab05.Hw2

import chisel3._
import chisel3.util.log2Ceil

class Cal_rfile(val depth: Int , val count_bit : Int = 150) extends Module {
    val io = IO(new Bundle {
    val push    = Input(Bool())
    val pop     = Input(Bool())
    val en      = Input(Bool())
    val reset    = Input(Bool())
    val dataIn  = Input(UInt(count_bit.W))
    val if_operand = Input(Bool())
    val dataOut = Output(UInt(count_bit.W))
    val if_operand_dataOut = Output(Bool())
    val empty   = Output(Bool())
    val full    = Output(Bool())
    })


    val rfile_mem = Mem(depth, UInt(count_bit.W))
    val rfile_ifOp_mem = Mem(depth, Bool())
    val sp        = RegInit(0.U(log2Ceil(depth+1).W))
    val out       = RegInit(0.U(count_bit.W))
    val ifOperand_out       = RegInit(false.B)


    when (io.en) {
        when(io.push && (sp < depth.asUInt)) {
        rfile_mem(0) := io.dataIn
        rfile_ifOp_mem(0) := io.if_operand
        for( i <- 0 to (depth-2)){
            rfile_mem(i+1) := rfile_mem(i)
            rfile_ifOp_mem(i+1) := rfile_ifOp_mem(i)
        }
        sp := sp + 1.U
        } .elsewhen(io.pop && (sp > 0.U)) {
        sp := sp - 1.U
        }
        when (sp > 0.U) {
        out := rfile_mem(sp - 1.U)
        ifOperand_out := rfile_ifOp_mem(sp - 1.U)
        }.otherwise{
            out := 0.U
            ifOperand_out := 0.U

        }
    }
    when(io.reset){
        out := 0.U
        ifOperand_out := 0.U
        sp := 0.U
    }
    io.empty := Mux(sp === 0.U, true.B, false.B)
    io.full:= Mux(sp === depth.asUInt , true.B, false.B)
    io.dataOut := out
    io.if_operand_dataOut := ifOperand_out
}
