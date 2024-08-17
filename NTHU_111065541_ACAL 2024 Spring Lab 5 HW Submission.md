NTHU_111065541_張騰午  ACAL 2024 Spring Lab 5 HW Submission 
===


###### tags: `AIAS Spring 2024` `Submission Template`



[toc]

## Gitlab code link


- Gitlab link - https://course.playlab.tw/git/Tiger_Chang/lab05/-/tree/main/hw5

## Hw5-1 TrafficLight with Pedestrian button
### Scala Code
> 請放上你的程式碼並加上註解(中英文不限)，讓 TA明白你是如何完成的。
```scala=
class TrafficLight_p(Ytime:Int, Gtime:Int, Ptime:Int) extends Module{
  val io = IO(new Bundle{
    val P_button = Input(Bool())
    val H_traffic = Output(UInt(2.W))
    val V_traffic = Output(UInt(2.W))
    val P_traffic = Output(UInt(2.W))
    val timer     = Output(UInt(5.W))
  })
  
  //parameter declaration
  val Off = 0.U
  val Red = 1.U
  val Yellow = 2.U
  val Green = 3.U

  val sIdle :: sHGVR :: sHYVR :: sHRVG :: sHRVY :: sPG :: Nil = Enum(6)

  //State register
  val state = RegInit(sIdle)
  val save_state = RegInit(sIdle)

  //Counter============================
  val cntMode = WireDefault(0.U(1.W))
  val cntReg = RegInit(0.U(4.W))
  val cntDone = Wire(Bool())
  cntDone := cntReg === 0.U

  when(io.P_button){//sPG時間
    cntReg := (Ptime-1).U
  }.elsewhen(cntDone){
    when(cntMode === 0.U){
      cntReg := (Gtime-1).U
    }.elsewhen(cntMode === 1.U){
      cntReg := (Ytime-1).U
    }
  }.otherwise{
    cntReg := cntReg - 1.U
  }
  //Counter end========================

  //Next State Decoder
  switch(state){
    is(sIdle){
      state := sHGVR
    }
    is(sHGVR){//若有P_button則依據現在state完成了沒存入save_state，
        //若本來state完成了存表訂下一個state，沒完成則存自己
      when(io.P_button) {
        save_state:=sHGVR
        state := sPG
      }.otherwise{
        when(cntDone) {state := sHYVR}
      }
    }
    is(sHYVR){
      when(io.P_button) {
        save_state:=sHYVR
        state := sPG
      }.otherwise{
        when(cntDone) {state := sHRVG}
      }
    }
    is(sHRVG){
      when(io.P_button) {
        save_state:=sHRVG
        state := sPG
      }.otherwise{
        when(cntDone) {state := sHRVY}
      }
    }
    is(sHRVY){
      when(io.P_button) {
        save_state:=sHRVY
        state := sPG
      }.otherwise{
        when(cntDone) {state := sHGVR}
      }
    }
    is(sPG){
      when(cntDone) {state := save_state}
    }
  }

  //Output Decoder
  //Default statement
  cntMode := 0.U
  io.H_traffic := Off
  io.V_traffic := Off
  io.P_traffic := Off

  switch(state){
    is(sHGVR){
      cntMode := 1.U
      io.H_traffic := Green
      io.V_traffic := Red
      io.P_traffic := Red
    }
    is(sHYVR){
      cntMode := 0.U
      io.H_traffic := Yellow
      io.V_traffic := Red
      io.P_traffic := Red
    }
    is(sHRVG){
      cntMode := 1.U
      io.H_traffic := Red
      io.V_traffic := Green
      io.P_traffic := Red
    }
    is(sHRVY){
      cntMode := 0.U
      io.H_traffic := Red
      io.V_traffic := Yellow
      io.P_traffic := Red
    }
    is(sPG){//save_state決定cntMode
      switch(save_state){
        is(sHGVR){
          cntMode := 0.U
        }
        is(sHRVG){
          cntMode := 0.U
        }
        is(sHYVR){
          cntMode := 1.U
        }
        is(sHRVY){
          cntMode := 1.U
        }
      }
      io.H_traffic := Red
      io.V_traffic := Red
      io.P_traffic := Green
    }
  }

  io.timer := cntReg

}
```
### Waveform
state	
sIdle:000	sHGVR:001	sHYVR:010	
sHRVG:011	sHRVY:100	sPG:101


![](https://course.playlab.tw/md/uploads/9136b116-bb37-4c57-b2b1-197720099e6d.png)
![](https://course.playlab.tw/md/uploads/4f006d43-28d5-4ef8-9980-a406846a10a2.png)
第一張圖沒有P_button一切照常進行，
第二張P_button輸入後若當時的state尚未完成則sPG結束後會切換回上一個state重新執行，
若當時的state能完成最後一個週期，則sPG結束後會直接進行下一個state執行，不會重複執行。















## Hw5-2-1 Negative Integer Generator
5-2根據助教說的可以做5-2-3交一份就好

## Hw5-2-2 N operands N-1 operators(+、-)


## Hw5-2-3 Order of Operation (+、-、*、(、))
- **如果你有完成Bonus部分，請在此註明。**
### Scala Code
> 請放上你的程式碼並加上註解(中英文不限)，讓 TA明白你是如何完成的。

```scala=
//CpxCal.scala
class CpxCal extends Module{
    val io = IO(new Bundle{
        val key_in = Input(UInt(4.W))
        val value = Output(Valid(UInt(32.W)))    
    })
    
    io.value.valid := false.B
    io.value.bits := 0.U

    //input轉數字與符號
    val get_and_store = Module(new GetandStore())
    get_and_store.io.key_in := io.key_in
    //數字符號in2post
    val in2post = Module(new My_In2post())
    in2post.io.input_data := get_and_store.io.getData.bits
    in2post.io.input_data_valid := get_and_store.io.getData.valid
    in2post.io.input_data_isnum := get_and_store.io.is_num

    //計算postfix
    val post_cal= Module(new Post_Cal())
    post_cal.io.input_data := in2post.io.in2post_dataOut.bits 
    post_cal.io.input_data_isnum := in2post.io.is_num_out
    post_cal.io.input_data_valid := in2post.io.in2post_dataOut.valid
    

    io.value.valid := post_cal.io.results.valid
    io.value.bits := post_cal.io.results.bits

}
```
code有點長放程式重要部分如下
```scala=
//GetandStore.scala
//分成6個states
//val sIdle :: sSrc :: sOp :: sLeft :: sRight :: sEqual :: Nil = Enum(6)

class GetandStore extends Module{
    val io = IO(new Bundle{
        val key_in = Input(UInt(4.W))
        val getData = Output(Valid(UInt(32.W)))
        val is_num = Output(Bool())          
    }) 

//......
    //Next State Decoder
    //依據接下來input決定下個state
    switch(state){
        is(sIdle){
            when(isoperator) {state := sLeft}              
            .elsewhen(isnum){ state := sSrc}
            .elsewhen(isleft){ state := sLeft}
        }
        is(sOp){
            when(isnum) {state := sSrc}
            .elsewhen(isleft){state := sLeft}
            .elsewhen(isequal){state := sEqual}    
        }
        is(sLeft){
            when(isnum){state := sSrc}
        }
        is(sSrc){
            when(isoperator){ state := sOp}
            .elsewhen(isequal){ state := sEqual}
            .elsewhen(isright){ state := sRight}
        }
        is(sRight){ 
            when(isoperator){ state:= sOp}
            .elsewhen(isequal){ state := sEqual}
        }
        is(sEqual){ state:=sIdle}
    }

    //Output Decoder
    switch(state){//在這個state output前一個state的data
        is(sIdle){
            when(pre_state === sEqual){
                io.getData.bits := 15.U
                io.getData.valid := true.B
                io.is_num := false.B
            }
        }
        is(sOp){
            when(pre_state === sSrc){
                io.getData.bits := src 
                io.getData.valid := true.B 
                io.is_num := true.B
            }.elsewhen( pre_state === sRight){
                is_neg := false.B//結束一個被括號包起來的operand，is_neg轉回false
                io.getData.bits := 14.U  
                io.getData.valid := !is_neg 
                io.is_num := false.B
            }.elsewhen(pre_state === sEqual){
                io.getData.bits := 15.U
                io.getData.valid := true.B
                io.is_num := false.B
            }
        }
        is(sLeft){//sLeft時遇到 - ，先不output，並用is_neg紀錄
            when(in_buffer1 === 11.U){
                is_neg := !is_neg
            }.elsewhen(pre_state === sOp){
                io.getData.bits := in_buffer2  
                io.getData.valid := true.B
                io.is_num := false.B
            }.elsewhen(pre_state === sLeft){
                io.getData.bits := 13.U  
                io.getData.valid := true.B
                io.is_num := false.B
            }
            when(pre_state === sEqual){
                io.getData.bits := 15.U
                io.getData.valid := true.B
                io.is_num := false.B
            }
        }
        is(sSrc){
            io.getData.valid := false.B
            when(pre_state === sSrc){//計算值
                src := (src<<3.U) + (src<<1.U) + in_buffer1
            }.otherwise{
                src :=  in_buffer1
            }
            when(pre_state === sOp){
                io.getData.bits := in_buffer2  
                io.getData.valid := true.B
                io.is_num := false.B
            }.elsewhen(pre_state === sLeft){
                io.getData.bits := 13.U 
                io.getData.valid := !is_neg//負數的括號不用output
                io.is_num := false.B
            }.elsewhen(pre_state === sEqual){
                io.getData.bits := 15.U
                io.getData.valid := true.B
                io.is_num := false.B
            }
        }
        is(sRight){
            io.getData.bits := Mux(pre_state === sRight, 14.U, Mux(is_neg, (~src).asUInt + 1.U, src ) )//is_neg decide是否取負數output
            io.getData.valid := true.B
            io.is_num := !(pre_state === sRight)
        }
        is(sEqual){
            io.getData.bits := MuxLookup(pre_state , 0.U, Seq(
                sOp -> in_buffer2,
                sLeft -> 13.U,
                sRight -> 14.U,
                sSrc -> src
            ))  
            io.getData.valid := Mux(pre_state === sRight, !is_neg, true.B )
            io.is_num := (pre_state === sSrc)

            when(pre_state =/= sEqual){
                in_buffer1 := 0.U
                in_buffer2 := 0.U
                src := 0.U
                is_neg := false.B 
            }
        }
    }
}
```
```scala=
//My_in2post.scala
//定義了一個queue行為的module
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
    val queue_isNum_mem = Mem(depth, Bool())
    val sp        = RegInit(0.U(log2Ceil(depth+1).W))
    val out       = RegInit(0.U(32.W))
    val isNum_out       = RegInit(false.B)


    when (io.en) {//在queue容量以內push進來放在0位置，所有值往+1搬一格
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

class My_In2post(keyin_size : Int =32) extends Module{
    val io = IO(new Bundle{
        val input_data = Input(UInt(keyin_size.W))
        val input_data_isnum = Input(Bool())
        val input_data_valid = Input(Bool())
        val in2post_dataOut = Output(Valid(UInt(keyin_size.W)))
        val is_num_out = Output(Bool())
    })
    
    //......
    //queue接收input data
    val in2post_queue = Module(new Queue_buffer(32))
    in2post_queue.io.dataIn := io.input_data
    in2post_queue.io.is_num := io.input_data_isnum
    in2post_queue.io.push := io.input_data_valid 
    val incoming_data = WireDefault(0.U(keyin_size.W))
    val incoming_isnum = Wire(Bool())
    incoming_data := in2post_queue.io.dataOut 
    incoming_isnum :=  in2post_queue.io.is_num_dataOut
    //queue的output為incoming data(現在要處理的資料)，可以push到stack
    val in2post_stack = Module(new Stack(32))
    in2post_stack.io.dataIn := incoming_data  
    val stack_top = WireDefault(0.U(keyin_size.W))
    stack_top := in2post_stack.io.dataOut
    
    val judge_result =RegInit(2.U(2.W)) //define judge_result

    when(incoming_data < 13.U  && incoming_data > 9.U && !incoming_isnum && stack_top < 13.U && stack_top > 9.U){
        when(incoming_data <= stack_top){judge_result := 0.U}//priority incoming_data<=stack top時
        .elsewhen(incoming_data > stack_top){judge_result := 1.U}//priority incoming_data>stack top時
    }.otherwise{judge_result := 2.U}

    when(!io.input_data_valid){//等到無input data時，
        when(incoming_isnum && !in_one_state){ //number output directly
            
            is_equal_state := false.B
            is_symbol_state := false.B
            is_left_state := false.B
            in2post_queue.io.pop := true.B
            out_value := in2post_queue.io.dataOut
            out_isnum := in2post_queue.io.is_num_dataOut
            out_valid := true.B
            in_one_state := true.B//一個number可能持續多個cycle，只需要output一次

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
                
            }.elsewhen( isright){//incoming是right bracket，pop stack並output直到pop出left bracket
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
                    is_symbol_state := true.B//pop一次就好
                    counter0 := 0.U
                }
            }.elsewhen( judge_result === 1.U || stack_top === 13.U){ //incoming優先級大於stack或stack top是left bracket-> push
                when(!is_symbol_state){
                    in2post_queue.io.pop := true.B
                    in2post_stack.io.push := true.B
                    is_symbol_state := true.B//push一次就好
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
                    is_left_state := !is_left_state//push一次就好
                 }
                           
            }.elsewhen(in2post_stack.io.empty ){
            //stack empty直接pop queue並push進去stack
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
```
```scala=
//Post_Cal.scala
class Post_Cal extends Module{
    val io = IO(new Bundle{
        val input_data = Input(UInt(32.W))
        val input_data_isnum = Input(Bool()) //true:opeand /false:symbol
        val input_data_valid  = Input(Bool())
        val results = Output(Valid(UInt(32.W)))
    }) 
    //buffer_queue接收input_data
    val buffer_que = Module(new Queue_buffer(32))
    buffer_que.io.dataIn := io.input_data
    buffer_que.io.is_num := io.input_data_isnum
    buffer_que.io.push := io.input_data_valid
    val incoming_data = WireDefault(0.U(32.W))
    val incoming_isnum = Wire(Bool())
    incoming_data := buffer_que.io.dataOut 
    incoming_isnum :=  buffer_que.io.is_num_dataOut
        //queue的output為incoming data(現在要處理的資料)，可以push到stack
    val pcal_stack = Module(new Stack(32))
    pcal_stack.io.dataIn := incoming_data  

    //......
    //postfix計算方式
    when(!io.input_data_valid ){
        counter := !counter//push一次換正負等queue有值
        when(counter){
            when(incoming_isnum){//number直接push in
                buffer_que.io.pop := true.B
                pcal_stack.io.push := true.B
                pcal_stack.io.pop := false.B
                io.results.valid  := false.B
                output_complete := false.B
                
            }.elsewhen(incoming_data =/= 0.U && !incoming_isnum){//非數字
                buffer_que.io.pop := false.B
                pcal_stack.io.push := false.B
                when(isequal){
                    when(output_complete){//output result過了不再output
                        io.results.bits := 0.U
                        io.results.valid  := false.B
                    }.elsewhen(!pcal_stack.io.empty){
                        //遇到 = 且stack未空就output結果
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

```
### Test Result
![](https://course.playlab.tw/md/uploads/3f8e910a-997d-4da6-a98a-ea7d463d4949.png)

![](https://course.playlab.tw/md/uploads/fc6885ae-51c1-4666-803b-61cbc6f394c5.png)

![](https://course.playlab.tw/md/uploads/79d900ac-5d3e-42e0-b121-1160d7e18475.png)

"(-10)+11+12-(-13)+(-14)="訊號從上到下可看到這個測資進入後可看到首先是Get_and_Store中state的變化，接著incoming_data是進入in2post buffer最前端的data，judge_result是incoming_data和stack top的priority比較結果，在00時stack top priority較大或等於，要pop並output，01代表可以直接push進入stack，而judge result為10時則為其他結果(數字或括號)。
in2post dataOut之後進入Post_Cal，也會先進入一個buffer，is_num為true的話為數字，直接push進入stack，遇到operator(is_num false)時會pop兩個數字出來做運算，算完push進入stack，直到計算完output


## Hw5-3-1 Pseudo Random Number Generator
### Scala Code
```scala=
object LfsrTaps {
    def apply(size: Int): Seq[Int] = {
        size match {
            // Seqp[Int] means the taps in LFSR
            case 4 => Seq(3)          //p(x) = x^4+x^3+1
            case 8 => Seq(6,5,4)      //p(x) = x^8+x^6+x^5+x^4+1
            case 16 => Seq(16,14,13,11)
            case _ => throw new Exception("No LFSR taps stored for requested size")
        }
    }
}
class PRNG(seed:Int) extends Module{
    val io = IO(new Bundle{
        val gen = Input(Bool())
        val puzzle = Output(Vec(4,UInt(4.W)))
        val ready = Output(Bool())
    })
    val sIdle :: sWait :: sCompu :: sCheck :: sOut :: Nil = Enum(5)
    io.puzzle := VecInit(Seq.fill(4)(0.U(4.W)))
    val pWire = Wire(Vec(4, UInt(4.W)))
    pWire := VecInit(Seq.fill(4)(0.U(4.W)))
    io.ready:=false.B
    val checkTable = RegInit(VecInit(Seq.fill(10000)(false.B)))
    

    val gen = RegNext(io.gen)

    val n = 16
    val shiftReg = RegInit(VecInit(Seq.fill(n)(false.B)))
    val PassReg = RegInit(0.U(2.W))

    val state = RegInit(sIdle)
    
    //Next State Decoder
    switch(state){
    is(sIdle){
        shiftReg zip seed.asUInt.asBools map {case(l,r) => l := r}
        io.ready:=false.B
        state := sWait
    }
    is(sWait){//等待訊號
        when(io.gen){
           state := sCompu
        }
    }
    is(sCompu){//計算state
        state := sCheck
    }
    is(sCheck){//檢查重複問題
        when(PassReg === 0.U){//去output
            state := sOut
        }.elsewhen(PassReg === 2.U){//回去產生新數
            state := sCompu
        }
    }
    is(sOut){//output state
        state := sWait
    }
    }

    //Output Decoder
    switch(state){
    is(sWait){
        io.ready := false.B
        PassReg:=1.U
    }
    is(sCompu){
        when(gen || PassReg === 2.U){
            (shiftReg.zipWithIndex).map{
            case(sr,i) => sr := shiftReg((i+1)%n)
            }
            //Fibonacci LFSR
            shiftReg(n-1) := (LfsrTaps(n).map(x=>shiftReg(n-x)).reduce(_^_))
        }
        
        PassReg:=1.U
    }
    is(sCheck){
        PassReg:=0.U//是否有重複數字
        for(i <- 0 until 3){
            for(j <- i+1 until 4){
                when(io.puzzle(i) === io.puzzle(j)){PassReg:=2.U}
            }
        }//check是否和前面出現過的重複
        when(checkTable(io.puzzle(0)*1000.U+io.puzzle(1)*100.U+io.puzzle(2)*10.U+io.puzzle(3))){PassReg:=2.U }
        checkTable(io.puzzle(0)*1000.U+io.puzzle(1)*100.U+io.puzzle(2)*10.U+io.puzzle(3)) := true.B//設為true
    }
    is(sOut){//sOut時拉起io.ready
        io.ready := true.B
    }
    }
    //處理shiftReg值，超過9就-10變回0~9的值
    val p0_ = Cat(shiftReg(3), shiftReg(2), shiftReg(1), shiftReg(0)).asUInt
    val p1_ = Cat(shiftReg(7), shiftReg(6), shiftReg(5), shiftReg(4)).asUInt
    val p2_ = Cat(shiftReg(11), shiftReg(10), shiftReg(9), shiftReg(8)).asUInt
    val p3_ = Cat(shiftReg(15), shiftReg(14), shiftReg(13), shiftReg(12)).asUInt
    
    pWire(0) := Mux(p0_ > 9.U, p0_ - 10.U, p0_)
    pWire(1) := Mux(p1_ > 9.U, p1_ - 10.U, p1_)
    pWire(2) := Mux(p2_ > 9.U, p2_ - 10.U, p2_)
    pWire(3) := Mux(p3_ > 9.U, p3_ - 10.U, p3_)
    io.puzzle := pWire

}
```
### Test Result
![](https://course.playlab.tw/md/uploads/2185b2db-9207-4d7a-919f-b01b60a4294f.png)
![](https://course.playlab.tw/md/uploads/20f9b8cc-a728-4a4e-a99d-2e75cf86d2b6.png)

產生數字後驗證是否有重複，有重複回到上個state(010)重新產生數字，再驗證，直到產生合法的數字為止才會拉起io.ready

## Hw5-3-2 1A2B game quiz
### Scala Code
> 請放上你的程式碼並加上註解(中英文不限)，讓 TA明白你是如何完成的。
```scala=
class NumGuess(seed:Int = 1) extends Module{
    require (seed > 0 , "Seed cannot be 0")

    val io  = IO(new Bundle{
        val gen = Input(Bool())
        val guess = Input(UInt(16.W))
        val puzzle = Output(Vec(4,UInt(4.W)))
        val ready  = Output(Bool())
        val g_valid  = Output(Bool())
        val A      = Output(UInt(3.W))
        val B      = Output(UInt(3.W))
     

        //don't care at Hw6-3-2 but should be considered at Bonus
        val s_valid = Input(Bool())
    })
    io.puzzle := VecInit(Seq.fill(4)(0.U(4.W)))
    io.ready  := false.B
    io.g_valid  := false.B
    io.A      := 0.U
    io.B      := 0.U

    val sIdle :: sWait :: sCompu :: sCheck :: sOut :: sGuess :: Nil = Enum(6)
    
    val pWire = Wire(Vec(4, UInt(4.W)))
    pWire := VecInit(Seq.fill(4)(0.U(4.W)))
    io.ready:=false.B
    val checkTable = RegInit(VecInit(Seq.fill(10000)(false.B)))
    

    val gen = RegNext(io.gen)

    val n = 16
    val shiftReg = RegInit(VecInit(Seq.fill(n)(false.B)))
    val PassReg = RegInit(0.U(2.W))
    val GuessReg = RegInit(0.U(3.W))
    val state = RegInit(sIdle)
    
    //Next State Decoder
    switch(state){
    is(sIdle){
        shiftReg zip seed.asUInt.asBools map {case(l,r) => l := r}
        io.ready:=false.B
        state := sWait
    }
    is(sWait){//等待訊號
        when(io.gen){
           state := sCompu
        }
    }
    is(sCompu){//計算
        state := sCheck
    }
    is(sCheck){//檢查重複問題
        when(PassReg === 0.U){//去output
            state := sOut
        }.elsewhen(PassReg === 2.U){//回去產生新數
            state := sCompu
        }
    }
    is(sOut){
        //state := sWait
        state := sGuess
    }
    is(sGuess){//sGuess state根據io.A訊號判定完成或是要重猜，重猜回sOut
        when(GuessReg === 5.U){
            state := state
        }.elsewhen(GuessReg === 4.U){
            state := sWait
        }.otherwise{
            state := sOut
        }
    }
    }

    //Output Decoder
    switch(state){
    is(sWait){
        io.ready := false.B
        PassReg:=1.U
        GuessReg:= 5.U
    }
    is(sCompu){
        when(gen || PassReg === 2.U){
            (shiftReg.zipWithIndex).map{
            case(sr,i) => sr := shiftReg((i+1)%n)
            }
            //Fibonacci LFSR
            shiftReg(n-1) := (LfsrTaps(n).map(x=>shiftReg(n-x)).reduce(_^_))
        }
        
        PassReg:=1.U
    }
    is(sCheck){
        PassReg:=0.U//是否有重複數字
        for(i <- 0 until 3){
            for(j <- i+1 until 4){
                when(io.puzzle(i) === io.puzzle(j)){PassReg:=2.U}
            }
        }//check是否和前面出現過的重複(1A2B game不需要)
        //when(checkTable(io.puzzle(0)*1000.U+io.puzzle(1)*100.U+io.puzzle(2)*10.U+io.puzzle(3))){PassReg:=2.U }
        //checkTable(io.puzzle(0)*1000.U+io.puzzle(1)*100.U+io.puzzle(2)*10.U+io.puzzle(3)) := true.B//設為true
    }
    is(sOut){//sOut時拉起io.ready
        io.ready := true.B
        GuessReg := 5.U
    }
    is(sGuess){//output guess result
        GuessReg := io.A
        io.g_valid:=true.B
    }
    }
    //處理shiftReg值，超過9就-10變回0~9的值
    val p0_ = Cat(shiftReg(3), shiftReg(2), shiftReg(1), shiftReg(0)).asUInt
    val p1_ = Cat(shiftReg(7), shiftReg(6), shiftReg(5), shiftReg(4)).asUInt
    val p2_ = Cat(shiftReg(11), shiftReg(10), shiftReg(9), shiftReg(8)).asUInt
    val p3_ = Cat(shiftReg(15), shiftReg(14), shiftReg(13), shiftReg(12)).asUInt
    
    pWire(0) := Mux(p0_ > 9.U, p0_ - 10.U, p0_)
    pWire(1) := Mux(p1_ > 9.U, p1_ - 10.U, p1_)
    pWire(2) := Mux(p2_ > 9.U, p2_ - 10.U, p2_)
    pWire(3) := Mux(p3_ > 9.U, p3_ - 10.U, p3_)
    
    io.puzzle := pWire
    //Both position & number correct
    val n0 = Mux(io.guess(3,0) === io.puzzle(0),1.U(3.W),0.U(3.W))
    val n1 = Mux(io.guess(7,4) === io.puzzle(1),1.U(3.W),0.U(3.W))
    val n2 = Mux(io.guess(11,8) === io.puzzle(2),1.U(3.W),0.U(3.W))
    val n3 = Mux(io.guess(15,12) === io.puzzle(3),1.U(3.W),0.U(3.W))

    io.A:=(n0 + n1 + n2 + n3)
    //數字正確卻不在正確position，判斷和非同position是否相同
    val b0 = (io.guess(3,0) === io.puzzle(1)) ^ (io.guess(3,0) === io.puzzle(2)) ^ (io.guess(3,0) === io.puzzle(3))
    val b1 = (io.guess(7,4) === io.puzzle(0)) ^ (io.guess(7,4) === io.puzzle(2)) ^ (io.guess(7,4) === io.puzzle(3))
    val b2 = (io.guess(11,8) === io.puzzle(0)) ^ (io.guess(11,8) === io.puzzle(1)) ^ (io.guess(11,8) === io.puzzle(3))
    val b3 = (io.guess(15,12) === io.puzzle(0)) ^ (io.guess(15,12) === io.puzzle(1)) ^ (io.guess(15,12) === io.puzzle(2))

    val b0_ = Mux(b0, 1.U(3.W),0.U(3.W))
    val b1_ = Mux(b1, 1.U(3.W),0.U(3.W))
    val b2_ = Mux(b2, 1.U(3.W),0.U(3.W))
    val b3_ = Mux(b3, 1.U(3.W),0.U(3.W))

    io.B:=(b0_ + b1_ + b2_ + b3_)

}
```
### Test Result
![](https://course.playlab.tw/md/uploads/094c4611-a41c-4896-9133-33651d03ac93.png)
![](https://course.playlab.tw/md/uploads/e263e42f-9557-418e-8031-bf928cb306db.png)
產生一個合法無重複數字的題目6801後，等guess值，接著進入sGuess(101)值判斷，並拉起g_valid根據結果若沒4A，回上個state重新等guess值，若有4A則結束。

## Bonus : 1A2B hardware solver [Optional]
### Scala Code
> 請放上你的程式碼並加上註解(中英文不限)，讓 TA明白你是如何完成的。
```scala=
## scala code & comment
```
### Test Result
> 請放上你通過test的結果，驗證程式碼的正確性。(螢幕截圖即可)


## 文件中的問答題
- Q1:Hw5-2-2(長算式)以及Lab5-2-2(短算式)，需要的暫存器數量是否有差別？如果有，是差在哪裡呢？
    - Ans1:短算式只需要src1,src2,op,in_buffer等reg就好，但長算式為了做出根據前一個state來判斷現在state要做的操作還要記錄負號，增加pre_state, neg等reg。
- Q2:你是如何處理**Hw5-2-3**有提到的關於**編碼衝突**的問題呢?
    - Ans2:除了output Valid這個訊號傳值與valid外同時多使用一個bool is_num訊號傳遞這個值是否是數字，以此判斷。
- Q3:你是如何處理**Hw5-3-1**1A2B題目產生時**數字重複**的問題呢?
    - Ans3:for迴圈檢查是否有一樣數字，有的話回到上個state重新產生數字。另外用一個10000 bit bool值reg checktable記錄所有產生過的亂數，若產生記錄過有產生過的，也回上個state重新產生數字
    ```
    is(sCheck){
        PassReg:=0.U//是否有重複數字
        for(i <- 0 until 3){
            for(j <- i+1 until 4){
                when(io.puzzle(i) === io.puzzle(j)){PassReg:=2.U}
            }
        }//check是否和前面出現過的重複
        when(checkTable(io.puzzle(0)*1000.U+io.puzzle(1)*100.U+io.puzzle(2)*10.U+io.puzzle(3))){PassReg:=2.U }
        checkTable(io.puzzle(0)*1000.U+io.puzzle(1)*100.U+io.puzzle(2)*10.U+io.puzzle(3)) := true.B
    }
    ```


## 意見回饋和心得(可填可不填)
這次深刻感受到chisel這類硬體語言和平常寫的sequential程式有非常大的不同，或許我還有很多不習慣吧，處理reg延遲、線路同時連接這類問題很傷腦筋，討論很多也查很多資料，但看同學一下幾行code就做完的事情我還要卡在一個明明就不該很難的問題上卡很久，就有點擔心之後怎麼辦，得早點適應了。
