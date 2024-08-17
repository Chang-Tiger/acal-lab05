---
title: ACAL 2024 Curriculum Lab 5 - Introduction to Chisel Programming - FSM Design
robots: noindex, nofollow
---
# <center>ACAL 2024 Curriculum Lab 5 <br /><font color="＃1560bd">Introduction to Chisel Programming<br /> FSM Design </font></center>
###### tags: `AIAS Spring 2024`
[TOC]

## Introduction
- 在課堂上，介紹了...
    - 電路行為的狀態圖(State Transition Diagram)繪製並以FSM的方式實現
    - FSM的基本組成
        - next state decoder (comb.)
        - output decoder (comb.)
        - state register (seq.)
    - FSM的兩大派別
        - Mealey：output只和當前state相關。
        - Moore：output和當前state以及input相關。
- 那在這次Lab中你會學習到：
    - 基本FSM的寫法，並以之實現複雜電路功能。

## Chisel-Related coding skill
:::info
- 建議同學可以先看chisel book的FSM部分。
:::
- 以下列舉了一些在實作FSM時常用到的語法
1. **Enumeration**
    - from [chisel3.util.Enum](https://www.chisel-lang.org/api/latest/chisel3/util/Enum.html)
    - code
        ```scala=
        //記得要import這個library
        import chisel3.util._
        
        val sIdle :: sMonday :: sTuesday :: sWendsday :: Nil = Enum(4)
        
        //你也可以用逐一宣告的方式...
        val sIdle = 0.U
        val sMonday = 1.U
        ...
        ```
        - 還記得preview，有放上兩篇介紹scala中有關各式**集合**的文章嗎？[Link 1](https://blog.csdn.net/qq_34291505/article/details/86832500)和[Link 2](https://vvviy.github.io/2018/12/12/Learning-Chisel-and-Scala-Part-II/)
            - Enum(4)會編號左邊list中每個元素，Dtype為UInt，從0.U開始。
            - Nil為list的結尾，不包含在list的“長度”中。
    - 所以在你使用條件判斷時，下面這兩行是等價的，但上面的判讀性就相對較好些。
        ```chisel=
        when (state === sIdle){...}
        when (state === 0.U){...}
        ```
3. **Conditional block**
    1. when-elsewhen-otherwise
        - format
            ```chisel=
            when(condition){
              //behavior
            }.elsewhen(condition){
              //behavior
            }.otherwise{
              //default behavior
            }
            ```
        :::info
        - reminder
            - chisel中：**等於**(**===**)和**不等於**(**=/=**)
            - elsewhen和otherwise前面要記得加上點(.)
        :::
    3. switch-is
        - format
            ```chisel=
            // default behavior
            
            switch(判斷對象){
              is(condition){
                //behavior
              }
              is(condition){
                //behavior
              }
              ...
            }
            
            ```
        :::info
        - reminder
            - switch-is本身沒有可以決定電路default行為的寫法，會建議大家在**前面**補上default behavior。
                - 補齊訊號隨條件改變的完整性，硬體合成時可以減少latch的出現。
                - default宣告在前面是因為chisel是越**後面**的宣告priority越大。
        :::
        
Lab5
===
## Lab5-0 : Environment and Repo Setup
- Build Course docker and bring up a docker container
    - 在開始lab之前必須先將課堂的docker container run起來，並把一些環境建好，可以參考下面的tutorial : [Lab 0 - Course Environment Setup](https://course.playlab.tw/md/33cXunaGSdmYFej1DJNIqQ)

:::warning
- You may setup passwordless ssh login if you like. Please refer to [Use SSH keys to communicate with GitLab](https://docs.gitlab.com/ee/user/ssh.html)
- Also, if you would like to setup the SSH Key in our Container. Please refer to this [document](https://course.playlab.tw/md/CW_gy1XAR1GDPgo8KrkLgg#Set-up-the-SSH-Key) to set up the SSH Key in acal-curriculum workspace.
:::

```shell=
## bring up the ACAL docker container 
## clone the lab05 files
$  cd ~/projects
$  git clone ssh://git@course.playlab.tw:30022/acal-curriculum/lab05.git
$  cd lab05

## show the remote repositories 
$  git remote -v
origin	ssh://git@course.playlab.tw:30022/acal-curriculum/lab05.git (fetch)
origin	ssh://git@course.playlab.tw:30022/acal-curriculum/lab05.git (push)

## add your private upstream repositories
## make sure you have create project repo under your gitlab account
$  git remote add gitlab ssh://git@course.playlab.tw:30022/<your ldap name>/lab05.git

$  git remote -v
gitlab	ssh://git@course.playlab.tw:30022/<your ldap name>/lab05.git (fetch)
gitlab	ssh://git@course.playlab.tw:30022/<your ldap name>/lab05.git (push)
origin	ssh://git@course.playlab.tw:30022/acal-curriculum/lab05.git (fetch)
origin	ssh://git@course.playlab.tw:30022/acal-curriculum/lab05.git (push)
```

- When you are done with your code, you have to push your code back to your own gitlab account with the following command :
```shell=
## the first time
$  git push --set-upstream gitlab main
## after the first time
$  git fetch origin main
## remember to solve conflicts
$  git merge origin/main
## then push back to your own repo
$  git push gitlab main
```
## Lab5-1 Traffic Light and 7-segment display controller
- 圖片來源：[pngimg](http://pngimg.com/download/56270), [MobiFlight Community Support](https://www.mobiflight.com/forum/topic/6637.html)
    ![](https://course.playlab.tw/md/uploads/9a1f32f0-8a6c-4f9a-952a-cead43956cce.png =28%x)![](https://course.playlab.tw/md/uploads/76df15c7-cd92-4754-a61c-40550bfc28c4.png =50%x)

### Introduction
- Lab5-1為實作FSM經典命題：紅綠燈，利用上禮拜教的Hardware Generator，決定綠燈以及黃燈的時長(兩者加起來時長為紅燈)，並將倒數時間以七段顯示器的方式呈現。
### Counter
- counter也是個小型的FSM，存值一直加一，直到數到最大限制時歸零。
- 你當然可以使用FSM來描述counter，但假設設計為0~7，那就會有8個狀態需要描述，但這8個狀態又有7個行為模式皆相同，倒不如直接描述暫存器的行為就好。
- Counter example code
    ```scala=
    counter = RegInit(0.U(3.W))
    counter := Mux(counter===7.U,0.U,counter+1.U)
    //or
    when(counter===7.U){counter:=0.U}
    .otherwise{counter:=counter+1.U}
    ```
### 7-segment display (七段顯示器)
- 將倒數時間具現化的工具
- 僅由Comb. circuit組成，若將Counter視為FSM，那麼此部分即為Output Decoder
- a~g為每一段的編號，而後面接的數字則表示在顯示哪數字時該段需變亮。
    - a：0、2、3、5、6、7、8、9
    - b：0、1、2、3、4、7、8、9
    - c：0、1、3、4、5、6、7、8、9
    - d：0、2、3、5、6、8、9
    - e：0、2、6、8
    - f：0、4、5、6、8、9
    - g：2、3、4、5、6、8、9
### Implement
- port declaration
    - Input
        - None
            - 電路reset後就不斷開始數，無外界因素影響FSM運作。
    - Output
        - H_traffic：水平路段的燈號呈現(Off：0 Red：1 Yellow：2 Green：3)
        - V_traffic：垂直路段的燈號呈現(Off：0 Red：1 Yellow：2 Green：3)
        - timer：倒數計時器
        - display：七段顯示器的7段訊號。
- state transition diagram
    - ![](https://course.playlab.tw/md/uploads/70201603-d696-4d79-bf16-3c43bf908dd5.png =35%x)
- Lab5-1 TrafficLight Code
    ```scala=
    class TrafficLight(Ytime:Int, Gtime:Int) extends Module{
      val io = IO(new Bundle{
        val H_traffic = Output(UInt(2.W))
        val V_traffic = Output(UInt(2.W))
        val timer     = Output(UInt(5.W)) 
        val display   = Output(UInt(7.W)) 
      })

      //parameter declaration
      val Off = 0.U
      val Red = 1.U
      val Yellow = 2.U
      val Green = 3.U

      val sIdle :: sHGVR :: sHYVR :: sHRVG :: sHRVY :: Nil = Enum(5)

      //State register
      val state = RegInit(sIdle)

      //Counter============================
      val cntMode = WireDefault(0.U(1.W))
      val cntReg = RegInit(0.U(4.W))
      val cntDone = Wire(Bool())
      cntDone := cntReg === 0.U

      when(cntDone){
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
        is(sHGVR){
          when(cntDone) {state := sHYVR}
        }
        is(sHYVR){
          when(cntDone) {state := sHRVG}
        }
        is(sHRVG){
          when(cntDone) {state := sHRVY}
        }
        is(sHRVY){
          when(cntDone) {state := sHGVR}
        }
      }

      //Output Decoder
      //Default statement
      cntMode := 0.U
      io.H_traffic := Off
      io.V_traffic := Off

      switch(state){
        is(sHGVR){
          cntMode := 1.U
          io.H_traffic := Green
          io.V_traffic := Red
        }
        is(sHYVR){
          cntMode := 0.U
          io.H_traffic := Yellow
          io.V_traffic := Red
        }
        is(sHRVG){
          cntMode := 1.U
          io.H_traffic := Red
          io.V_traffic := Green
        }
        is(sHRVY){
          cntMode := 0.U
          io.H_traffic := Red
          io.V_traffic := Yellow
        }
      }

      io.timer := cntReg

      val ss = Module(new SevenSeg())
      ss.io.num := cntReg
      io.display := ss.io.display
    }
    ```
- 接著執行這條指令...
    ```shell=
    $ sbt 'Test/runMain acal_lab05.Lab1.TrafficLightTest'
    ```
- 結果...
    ![](https://course.playlab.tw/md/uploads/52b4f34c-ed44-4266-a270-d734108d0705.png)
    - 綠燈時長在tester裡設為7，為6~0
    - 黃燈時長在tester裡設為3，為2~0

Lab5-2 Arithmetic Calculator
---
### Introduction
- 此次**作業**希望同學可以實現擁有加減乘功能的且遵守四則運算規則的計算機，輸入算式的方式會以series輸入，同學需根據不同的電路”狀態“來因應當前輸入信號的處理。
- 由Lab5-2開始入門，作業再進行後續延伸。
    - Level_1 Integer Generator
    - Level_2 2 operands 1 operator(+、-、*)
- HomeWork
    - Level_3 negative Integer Generator
    - Level_4 N operands N-1 operators(+、-)
    - Level_5 Order of Operation (+、-、*、(、))
- 數字鍵符號鍵和硬體輸入訊號的mapping，剛好4bits就能完整表示我們需要的input：

| input |  0  | 1   | 2   | 3   | 4   | 5   | 6   | 7   | 8   | 9   | +   | -   | *   | (   | )   | =   |
|:-----:|:---:| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
|  HW   | 0x0 | 0x1 | 0x2 | 0x3 | 0x4 | 0x5 | 0x6 | 0x7 | 0x8 | 0x9 | 0xA | 0xB | 0xC | 0xD | 0xE | 0xF |


Lab5-2
----
### Lab5-2-1 Integer Generator
#### Introduction
- 一台功能正常的計算機，當你輸入常數按下等號後，必會顯示出剛剛輸入的數字。
    ```
    Input: 1 2 3 4 =
    Output: 1234
    ```
- 此次lab希望同學能夠依照輸入順序的權重將輸入組合成期望的數字。
- State declaration
    - **sIdle**：reset中。
        - <font color=#888>切換至sAccept：reset訊號結束的下一刻電路開始運作。</font>
    - **sAccept**：接收輸入並組合數字。
        - <font color=#888>切換至sEqual：接收到"=(15)"時</font>
    - **sEqual**：
        1. 清空number暫存器，以準備下一次的輸入。
        2. 顯示組合出的數字，並將Valid訊號設為true。
        3. <font color=#888>再次切換至sAccept，準備開始接受下一筆測值。</font>
    :::info
    1. 電路設計中，難以避免的是I/O port上隨時都有值，只能依靠**ready-valid協議**來讓彼此相連的兩個blocks知道目前在port上的資料是否可以取用。
        - 在更複雜的chisel設計中，常會見得在I/O port與Data type的宣告間，多包了一層**Valid**(沒有ready)、或者是**DecoupledIO**(如下面程式碼的第4行)，一旦包上，在取值上會與原本的語法有些不同，必須得多加上以下三項，才能取到或賦值給你想要的wire(下面程式碼Line36、37)。
            - ready(input)：接受來自下一級的ready訊號，(下一級電路)向上一級準備好接受下一筆資料了。
            - valid(output)：傳遞給下一級表示目前port上這筆資料為可以取用。
            - bits：data
    2. 乘以10的實現方式(line 29)
        - 電路設計中，若寫 \*10，代價會是合成出一**乘法器**，嚴重影響硬體面積的罪魁禍首...乘法器的出現應該只有在電路需要很general的乘法時才不得不出現(ex：快速傅立葉轉換)，若單單只需要\*10，應以shift和add取而代之。
    :::
    
    :::info
    ###  __DecoupledIO的用法__
    > Decoupled 通常用於 module 輸出端訊號的宣告
    > 因為如果利用 Decoupled 宣告某一個 IO Port，則 Chisel 會編譯生成以下電路
    > 
    > ![](https://pic2.zhimg.com/v2-1cc2bb62e3fd71cd4bacc09b4a9af835_r.jpg)
    >
    > 其中原本的輸出訊號會包含以下三個部分
    > 
    > - `io.outSignal.bits`
    > - `io.outSignal.valid`
    > - `io.outSignal.ready`
    > 
    > 而 `bits` 和 `valid` 的屬性都是 `Output`，但要注意 `ready` 的屬性為 `Intput`。通常 `ready` 訊號會接收下一級（next stage）電路傳來的 ready 訊號，只有當下一級電路的 ready 訊號和目前模組的 valid 訊號同為 High 時，該模組的輸出信號才會被下一級電路採用
    > （所以才會被稱為握手協議，因為只有當我的 valid 和他的 ready 都為 High 時，我的輸出才會被他所採用，就像是我們兩個都各自想和對方握手，我們才能握手一樣。）
    >
    > 宣告 IO Port 的時候，利用以下方式宣告
    > 
    > ```scala=
    > val io = IO(new Bundle{
    >    outSignal = Decoupled(Output(Dtype(width.W)))
    >    // ...
    >})
    > ```
    > 
    > 其中 `Dtype` 是訊號類型，例如 `UInt`、`Bool` 等等...
    > 
    > 更多用法舉例可以參考：[Decoupled用法](https://blog.csdn.net/PP_forever/article/details/95977959)
    :::
    
    :::info
    > __乘以 10 方法的解釋__
    > 
    > 在下面助教的範例程式裡面寫到
    > 
    > ```scala=
    > number := (number<<3.U) + (number<<1.U) + in_buffer
    > ```
    >
    > 他的含義其實就是，往左 shift 3-bits 等同於乘以8，而往左 shift 1-bits 等同於乘以2，所以兩者相加就等價於乘以10。
    :::
    
- Hardware Circuit Overview
![](https://course.playlab.tw/md/uploads/e8998eae-100f-4df8-a5f8-4b4488e135cc.png =90%x)


- Lab5-2-1 IntGen.scala code
    ```scala=
    class IntGen extends Module{
        val io = IO(new Bundle{
            val key_in = Input(UInt(4.W))
            val value = Output(Valid(UInt(32.W)))
        })

        val equal = WireDefault(false.B)
        equal := io.key_in === 15.U

        val sIdle :: sAccept :: sEqual :: Nil = Enum(3)
        val state = RegInit(sIdle)
        //Next State Decoder
        switch(state){
            is(sIdle){
            state := sAccept
            }
            is(sAccept){
            when(equal) {state := sEqual}
            }
            is(sEqual){
                state := sAccept
            }
        }

        val in_buffer = RegNext(io.key_in)

        val number = RegInit(0.U(32.W))
        when(state === sAccept){
            number := (number<<3.U) + (number<<1.U) + in_buffer
        }.elsewhen(state === sEqual){
            number := 0.U
        }.otherwise{
            number := number
        }

        io.value.valid := Mux(state === sEqual,true.B,false.B)
        io.value.bits := number
    }
    ```
- 接著執行這條指令...
    ```shell=
    $ sbt 'Test/runMain acal_lab05.Lab2.IntGenTest'
    ```
- 結果...
    ![](https://codimd.playlab.tw/uploads/upload_e7ec23b80cd576c6e5ddc61083a736c3.png)

### Lab5-2-2 2 operands 1 operator
#### Introduction
- 接續5-2-1，能夠組合出數字後，接著根據輸入的運算子，使兩個不同的運算元去做想要的運算。
    ```
    Input：1 2 3 4 - 2 3 4 =
    Output：1000
    ```
- State declaration
    - **sIdle**：reset中。
        - <font color=#888>切換至sAccept：reset訊號結束的下一刻電路開始運作。</font>
    - **sSrc1**：接收輸入並組合Src1。
        - <font color=#888>切換至sOp：接收到"+(10)、-(11)、*(12)"時</font>
    - **sOp**：接收輸入設定運算方式。
        - <font color=#888>切換至sSrc2：接收到"0~9"時</font>
    - **sSrc2**：接收輸入並組合Src2。
        - <font color=#888>切換至sEqual：接收到"=(15)"時</font>
    - **sEqual**：
        1. 清空number暫存器，以準備下一次的輸入。
        2. 顯示組合出的數字，並將Valid訊號設為true。
        3. <font color=#888>切換至sSrc1</font>
- Hardware Circuit Overview
    ![](/md/uploads/f414ee5b-111b-423f-8595-af6413939463.png)

上圖紅色的mux在設計上會採用2bits mux，第一個bit用來表示`state == sSrc_ (or state == sOp)`，第二個bit用來表示`state == sEqual`，可以參考下面表格，可以注意的是當第二個bit(msb bit)為1時，後面則是x表示`don't care`。

| bits | select_value               |
| ---- | -------------------------- |
| 00   | src_ (or Op) reg中本來的值   |
| 01   | ALU (or Adder) output      |
| 1x   | 0                          |

- Lab5-2-2 EasyCal.scala code
    ```scala=
    class EasyCal extends Module{
        val io = IO(new Bundle{
            val key_in = Input(UInt(4.W))
            val value = Output(Valid(UInt(32.W)))
        })
        //Wire Declaration===================================
        val operator = WireDefault(false.B)
        val num = WireDefault(false.B)
        val equal = WireDefault(false.B)
        operator := io.key_in >= 10.U && io.key_in <= 12.U
        num := io.key_in < 10.U
        equal := io.key_in === 15.U

        //Reg Declaration====================================
        val in_buffer = RegNext(io.key_in)
        val src1 = RegInit(0.U(32.W))
        val op = RegInit(0.U(2.W))
        val src2 = RegInit(0.U(32.W))
        //State and Constant Declaration=====================
        val sIdle :: sSrc1 :: sOp :: sSrc2 :: sEqual :: Nil = Enum(5)
        val add = 0.U
        val sub = 1.U
        val mul = 2.U 
        
        //Next State Decoder
        val state = RegInit(sIdle)
        switch(state){
            is(sIdle){
                state := sSrc1
            }
            is(sSrc1){
                when(operator) {state := sOp}
            }
            is(sOp){
                when(num) {state := sSrc2}
            }
            is(sSrc2){
                when(equal) {state := sEqual}
            }
            is(sEqual){
                state := sSrc1
            }
        }
        //==================================================

        when(state === sSrc1){src1 := (src1<<3.U) + (src1<<1.U) + in_buffer}
        when(state === sSrc2){src2 := (src2<<3.U) + (src2<<1.U) + in_buffer}
        when(state === sOp){op := in_buffer - 10.U}
        when(state === sEqual){
            src1 := 0.U
            src2 := 0.U
            op := 0.U
        }

        io.value.valid := Mux(state === sEqual,true.B,false.B)
        io.value.bits := MuxLookup(op,0.U,Seq(
            add -> (src1 + src2),
            sub -> (src1 - src2),
            mul -> (src1 * src2)
        ))
    }
    ```
- 接著執行這條指令...
    ```shell=
    $ sbt 'Test/runMain acal_lab05.Lab2.EasyCalTest'
    ```
- 結果...
    ![](https://course.playlab.tw/md/uploads/ae6b3009-3fb4-4e15-be6f-c61733ac5e2a.png)

## Lab5-3 LFSR (Linear Feedback Shift Register)
### Introduction
* linear-feedback shift register (LFSR) is a shift register whose input bit is a linear function of its previous state.
    - is used for Pseudo Random Numbers in Digital Hardware to implement a finite state machine.
* The most commonly used linear function of single bits is exclusive-or (XOR).
### Example:
- 下圖為一個 4-bit LFSR, 初始為0001
    ![](https://course.playlab.tw/md/uploads/efa2f759-e2a3-4b80-9277-9fb5a13cd682.png =80%x)

上圖可以看到此LFSR首先先右移1 bit再將原本第3 4 bit做XOR之後取代第一個bit變成1000, 再重複以上的動作, 結果如下圖
    ![](https://course.playlab.tw/md/uploads/3bbddeec-651e-4b7c-83e7-5fff9e561a88.png)

可以發現經過15次後又回到了初始值, 並且可以看到在這15次中每次的值都不一樣, 也代表4 bit所有可能都走過一遍, 如此, 可稱此LFSR為Maximum-length LFSR
### 思考
- Q1. 如果初始改為0010還會是Maximum-length LFSR嗎
- Q2. 如果初始為0000還可以work嗎
### Note
- 此硬體的功能實現較為容易，設計也較為自由
    - shift reg的長度(個數)
    - tap有多少個，位置
- reference
    * [Wikipedia article on Linear feedback shift registers.](http://en.wikipedia.org/wiki/Linear_feedback_shift_register)
    * [A Random Number Generator in Verilog](http://rdsl.csit-sun.pub.ro/docs/PROIECTARE%20cu%20FPGA%20CURS/lecture6%5B1%5D.pdf)

### Lab5-3-1 Fionacci version

- 提供的程式碼，讓使用者能夠根據傳入參數(n)的不同選擇(4、8)兩種不同size的lfsr
- Lab5-3/LFSR_Fibonacci.scala code
    ```scala=
    object LfsrTaps {
        def apply(size: Int): Seq[Int] = {
            size match {
                // Seqp[Int] means the taps in LFSR
                case 4 => Seq(3)          //p(x) = x^4+x^3+1
                case 8 => Seq(6,5,4)      //p(x) = x^8+x^6+x^5+x^4+1
                case _ => throw new Exception("No LFSR taps stored for requested size")
            }
        }
    }

    class LFSR_Fibonacci (n:Int)extends Module{
        val io = IO(new Bundle{
            val seed = Input(Valid(UInt(n.W)))
            val rndNum = Output(UInt(n.W))
        })
        
        //ShiftReg的初始化
        val shiftReg = RegInit(VecInit(Seq.fill(n)(false.B)))
        
        //傳入seed，將seed值放上ShiftReg
        when(io.seed.valid){
          shiftReg zip io.seed.bits.asBools map {case(l,r) => l := r}
        }.otherwise{
        
          //Barrel Shift Register
          (shiftReg.zipWithIndex).map{
            case(sr,i) => sr := shiftReg((i+1)%n)
          }
          
          //Fibonacci LFSR
          shiftReg(n-1) := (LfsrTaps(n).map(x=>shiftReg(n-x)).reduce(_^_)) ^ shiftReg(0)
        }
        io.rndNum := shiftReg.asUInt
    }
    ```
    :::info
    - 解釋(Line 32)：
        - shiftReg(n-1)的下一個值來源和其他的暫存器不同，所以覆寫掉上面barrel shifter寫的，記住：後寫的priority較大。
        - LfsrTap(4)，會回傳一Seq，為Seq(3)
        - **map指令**為Higher-order function，map(x=>shiftReg(n-x))將Seq(3)變成了Seq(shiftReg(1))
        - **reduce指令**也同為Higher-order function，主要工作為針對集合裡的所有元素做單一運算(比如說sum、&&)，在此範例中是^(XOR)，最後會匯集回傳一個值。
    :::
- 接著執行這條指令...
    ```shell=
    $ sbt 'Test/runMain acal_lab05.Lab3.LFSR_FibonacciTest'
    ```
- 結果...
    ![](https://course.playlab.tw/md/uploads/16e301c4-0b4e-4912-857f-c58c7736f8e1.png)

### Lab5-3-2 Galois version
- 另一種產生隨機數的方式
- 硬體實現的優點：有較短的critical path
- Lab3/LFSR_Galois.scala code
    ```scala=
    class LFSR_Galois (n:Int)extends Module{

        val io = IO(new Bundle{
            val seed = Input(Valid(UInt(n.W)))
            val rndNum = Output(UInt(n.W))
        })

        val shiftReg = RegInit(VecInit(Seq.fill(n)(false.B)))

        when(io.seed.valid){
          shiftReg zip io.seed.bits.asBools map {case(l,r) => l := r}
        }.otherwise{

          //Right Barrel Shift Register
          (shiftReg.zipWithIndex).map{
              case(sr,i) => sr := shiftReg((i+1)%n)
          }
          //Galois LFSR
          LfsrTaps(n).map{x => {shiftReg(x-1) := shiftReg(x) ^ shiftReg(0)}}
        }
        io.rndNum := shiftReg.asUInt
    }
    ```
    :::info
    - 上面兩個lab放在同個package裡面，所以直接引用LfsrTaps(n)即可。
    :::
- 接著執行這條指令...
    ```shell=
    $ sbt 'Test/runMain acal_lab05.Lab3.LFSR_FibonacciTest'
    ```
- 結果
    ![](https://course.playlab.tw/md/uploads/403d9fb8-a54d-4684-980e-68688d1e276b.png)

Homework 5
====
Hw5-1 TrafficLight with Pedestrian button
---
#### Introduction
- Lab5-1做出了僅考慮了十字路口中的水平以及垂直車流，而此項作業希望同學能將行人通行的狀態加入至原本的FSM中。並新增了**行人通行按鈕**的功能。
    - 行人通行狀態(**sPG**)，請加入至Lab5-1狀態圖中sHRVY和sHGVR之間，且時長由一新增傳入參數`Ptime`決定。
- **行人通行按鈕**`P_button`功能描述：
    1. 按下當下狀態非行人通行狀態(sPG)：無論在哪種狀態，下一刻立即切換至行人通行狀態，並維持`Ptime`時間。結束之後，便切回按下按鈕那一刻的狀態，重新倒數，並維持原先排程。
        - Ex.我在sHGVR的時候按下了p_button，則下一刻會變成行人通行狀態維持`Ptime`秒後，狀態切回sHGVR且**重新倒數**，準備切換至sHYVR。
    2. 按下當下狀態為行人通行狀態(sPG)：並無功能。
- 測試要求：
    1. 雖然時長由傳入參數決定，但為了驗證各位同學的電路功能，時長設定統一如下...，完整週期共會花費25秒。這部分已在tester中設定，同學不用做什麼更動。
        - Ytime = 3
        - Gtime = 7
        - Ptime = 5
    2. 請同學在繳交文件上附上**vcd檔截圖**兩段(以下說明)，圖上至少須包含以下信號...
        - state
        - timer
        - H_traffic
        - V_traffic
        - P_traffic
        - p_button
    3. 兩段截圖：
        - 第一段：前25個週期，為不受`P_button`干擾的燈號運行，理應飽含了紅綠燈的完整一次的循環(共25個週期)。以下為截圖示範...
        ![](https://course.playlab.tw/md/uploads/67f23f6f-edd8-4704-95b5-cc64acf3e297.png)
        - 第二段：在`P_button`的影響下，一樣截圖紅綠燈運行的完整週期(會較長...)，仔細看`P_button`不同的時間點的觸發對電路的影響。
        ![](https://course.playlab.tw/md/uploads/e6dbe8e5-331b-45d8-a17d-62d2c57307a0.png)

        - **設計方式可以和助教不相同**，但請在文件上說明詳細，圖上的訊號資訊。
        - 比如說關於state的輸出信號分別對應了哪個state，以上圖為例，在說明文件上我就會附上以下說明：
          | state | sIdle | sHGVR | sHYVR | sHRVG | sHRVY | sPG |
          |:-----:|:-----:|:-----:|:-----:|:-----:|:-----:|:---:|
          |  map  |   0   |   1   |   2   |   3   |   4   |  5  |
- port declaration (可以以Lab5-1去做更動實現)
    ```scala=
    class TrafficLight_p(Ytime:Int, Gtime:Int, Ptime:Int) extends Module{
        val io = IO(new Bundle{
            val P_button = Input(Bool())
            val H_traffic = Output(UInt(2.W))
            val V_traffic = Output(UInt(2.W))
            val P_traffic = Output(UInt(2.W))
            val timer     = Output(UInt(5.W)) 
        })
    }
    ```
- 執行指令...
    ```shell=
    $ sbt 'Test/runMain acal_lab05.Hw1.TrafficLight_pTest'
    ```
Hw5-2 Calculator
---
### preface
- 5-1-(1~3)：沿用Lab5-2在不同狀況下的思維與設計，使計算機的功能更加完善。
:::danger
- 注意：為因應多條測資的可能性，同學在設計FSM的時候，輸出答案的下一個狀態應回復至準備接收下一筆測資的狀態，具體請見Lab5-2 module 以及 tester。
:::
### Hw5-2-1 Negative Integer Generator
#### Introduction
- Lab5-2-1讓同學實作出Number Generator，而此項作業則要求同學能夠加以沿用，讓負數也一樣能夠被組合出來，同時也能向下兼容Lab5-2-1的功能。
- tester input
    ```
    //Situation 1
    Input：( - 1 2 3 4 ) =
    Output：-1234

    //Situation 2
    Input：1 2 3 4 =
    Output：1234
    ```
:::warning
- 要求：Hw5-1的實作中，對於負數的格式要求，必須<font color=#f00>**以括號包起來**</font>，難度會降低許多。
    - Hint：<font color=#f00>( -</font> 1 2 3 4 ) =
    - 有些部分勢必得記下來，才可以影響如何存值。
:::
- Hardware Circuit Overview
    ![](https://course.playlab.tw/md/uploads/a05717d6-6e79-40c7-a947-222bdeaa03e5.png)

- Port declaration
    ```scala=
    class NegIntGen extends Module{
        val io = IO(new Bundle{
            val key_in = Input(UInt(4.W))
            val value = Output(Valid(UInt(32.W)))
        })
    }
    ```
- 執行指令...
    ```shell=
    $ sbt 'Test/runMain acal_lab05.Hw2.NegIntGenTest'
    ```
### Hw5-2-2 N operands N-1 operators(+、-)
#### Introduction
- 在Lab5-2-2我們考慮了兩個運算元和一個運算子的狀況，本次作業希望以Lab為基底，將功能延伸至N個運算元{N:N>0}和N-1個運算子的計算。並能向下兼容Lab5-2以及Hw5-1-1的功能。
- **四則運算**最基本的三個原則：
    1. 先乘除後加減 -> 考慮到operator priority
    2. 括號內先算
    3. 由左算至右
- 此項作業僅考慮第三個條件做處理，處理長算式中一系列的**加減運算**。
- tester input
    ```
    //Situation 1
    Input：11 + 12 + 3 + 14 + 15 + 16 =
    Output：71

    //more difficult
    Input：1 - ( - 12 ) + 3 + 4 + 5 - ( - 6 ) =
    Output：31
    
    //downward compatibility
    Input：( - 12 ) =
    Output：-12
    ```
- Port declaration
    ```scala=
    class LongCal extends Module{
        val io = IO(new Bundle{
            val key_in = Input(UInt(4.W))
            val value = Output(Valid(UInt(32.W)))
        })
    }
    ```
- State Overview
![](https://course.playlab.tw/md/uploads/a09045be-7753-4f9d-a15a-aa27e66b0cba.png =25%x)

- 執行指令...
    ```shell=
    $ sbt 'Test/runMain acal_lab05.Hw2.LongCalTest'
    ```
### Hw5-2-3 Order of Operation (+、-、*、(、))
#### Introduction
- 不同於前面的Lab，在**運算子、運算元的數量**和**運算順序 由左至右**都是已知、確定的情況下，自然可以簡單地用state diagram來做出輸入值該存至哪個暫存器的區分(sSrc1、sOp、sSrc2)
- 此項作業希望同學將四則運算的三項原則都考慮進來，分析在一條算式中運算的執行順序，並制定對應的FSM。
- Order of Operation：
    1. Parentheses
    2. <font color=#aaa> Exponents (right to left) 此項作業不考慮，列出僅供參考。</font>
        - right to left的意思是右邊的優先權會高於左邊的
        - Ex: 2 ^ 3 ^ 2=2^9=512
    4. Mutiplication (left to right)
    5. Addition and Subtraction (left to right)
- 人類在解算式，習慣以**中序infix**方式來解讀並計算，但對於電腦來說，受限於演算法無法綜觀並處理整條算式的緣故，習慣將表示方式轉成**後序postfix**後再作運算。[reference](https://www.javatpoint.com/convert-infix-to-postfix-notation)。
  :::info
  Example：
  - infix: 4+3+(-1)-6
  - postfix: 43+(-1)+6-
  - 表示方式的小小提醒：(-1)是視為一個運算元喔!也可以寫作0xffffffff，但因為太醜了，所以這邊提醒一下，括號只是讓同學好區分負數正數，不論是infix還是postfix都僅記下了4個運算元和3個運算子。
  :::
- 需要重新考慮的有：
    1. state的設計劃分
    2. 在infix2postfix之中，stack的運用
        - 參考資源：chisel-tutorial/src/main/scala/examples/Stack.scala
        - **可以自行多加**檔案 stack.scala並依自己設計做更動，繳回作業時一併上傳至gitlab即可。
        - ![](https://codimd.playlab.tw/uploads/upload_ecfe5e656d5433965f8baded3f4d7c77.png =20%x)
    3. 若是採用先存值再處理的方式。編碼衝突該如何處理? 運算元10~15都會遇到和符號編碼相衝突，該如何解決呢？

- 設計參考：
    - Top-level state劃分：
    	- **Store** - 把 parse 好的 source operand 值存下來
		- **InToPost** - 把formula 從Infix 的順序轉成Postfix 的順序
		- **Calculate** - 做計算
    ![](https://course.playlab.tw/md/uploads/f5cffdc1-9dde-4a0d-8f8f-5d238aefb1c0.png)

- Look more in details 
	:::info 
	1. 上面top-level states的劃分跟硬體的圖，同學可以當作參考就好。top-level states是這題最重要要做的三件事，至於裡面的細節或要再細分不同的state，同學可以自己規劃。

	2. 事實上，以這3個state來說，並不需要等到前一級的state做完，可以在前一級Input進來時就可以做這一個state的事情。如果要等到前一級state完全做完下一級這個state才能做事就會遇到internal buffer或stack長度不夠的問題。下面放上上個學期當作bonus的測資，同學可以來挑戰看看測試能不能通過這些test pattern。
	```
	Input1: ("(15-8)*(2+9)-(15-(-15)*3+8)*(-10)=", 757),
	Input2: ("(((((-12)+8)*((5-1)*((-3)*9)-3)+1)-(-3))*4*(5-3)-3)=",3581),
	Input3: ("((-123)*((-32)+3)*4+(15-(-16)))*(((-4)-2)*((-2)+1))=",85794),
    Input4: ("(5+3)*(7-8)+((-3)*(7+3)*(8+9)-7)*((3+4)-3)=",-2076),
	val num = BigInt("5360161621086697477532205257084500572")
    Input5: ("(((((((((8-3)*2-4)*3-2)*4-1)*3+5)*2-1)*3+2)*2+4)*3+8)*4-1234567890*98271811098-244817292034*(674373294052-3472781923742)*7823924729230=",num))
	```
	:::

- tester input
    ```
    //Situation
    Input：1 1 * ( 1 2 - 3 ) * 1 4 + ( 1 5 - ( - 1 6 ) )=
    postfix:??
    Output:1417
    ```
- Port declaration
    ```scala=
    class CpxCal extends Module{
        val io = IO(new Bundle{
            val key_in = Input(UInt(4.W))
            val value = Output(Valid(UInt(32.W)))
        })
    }
    ```
:::info
- General Calculator
    - 最後做出來的計算機應能夠兼容前面所有Lab、Hw它們tester裡面的算式。
:::
#### **Bonus**
- 在不**影響作業要求**以及**更動原本編碼**的情況下，同學可以選擇...
  1. 將“＾”功能實現(Order of Operation 2.)
      - 記得要擴充key_in的bit數，和tester裡面的dict
        ![](https://course.playlab.tw/md/uploads/9cf4dc71-f932-4ffa-84c1-56bf35681ca1.png =15%x)
      - 自行加入算式作為測值，並在繳交文件中註明你有做到的額外功能。
      - 算式格式：
          - 字串，中間必須無空格
          - 以等號做結尾，ex: <font color=#075>"(-15)-15-(-15)+(-15)="</font>
          - 算式要合理，括號數量要對！
  2. 使功能更便利...
      - 比如說，負數可以不需要括號就能夠實現，當然有括號的還是得能夠正確
          - 一樣將“額外功能”在說明文件上註明，並加上算式在tester中。

Hw5-3 LFSR-base 1A2B Number Guess Game
---
### preface
- 利用LFSR(Linear feedback shift register)，作為隨機數字產生器，並將其應用至經典1A2B(Bulls and Cows)。
    ![](https://course.playlab.tw/md/uploads/1ae4f419-9f99-454a-a4e0-7ddaffcfee79.png =80%x)
- 電路行為描述：
    - Reset後，LFSR根據seed做為初始值，每一次clock做一次shift。
    - Gen為採樣訊號，拉至High時，會採樣LFSR當下**處理好**的值(ex:1834)。 <- Hw5-3-1
    - 當數列產生無誤後，`ready`訊號會拉至High讓使用者知道可以開始猜。
    - `Guess[15:0]`為玩家的輸入，和題目一樣每4個bits代表一隨機數。
    - 輸入`Guess`後，電路開始比較**題目**以及**猜測**，`ready`訊號會拉至Low。，除此之外，`ready`訊號也會在最終結果(幾A幾B)和`valid`輸出後的下一刻，再次拉高至High等待下一筆猜測。
    - 計算完成後，電路會將`valid`訊號拉至High，並同時將幾A幾B結果輸出。
    - 當計算結束後，除非A=4，否則電路又會將`ready訊號`拉至High，等待下一次的猜測。

### Hw5-3-1 Pseudo Random Number Generator

:::warning
>在作業原始碼中，有關於 io.puzzle 信號的初始化
>原本的程式碼是
>
>```scala=
>io.puzzle := Vec(4, 0.U)
>```
>
>但這樣的寫法在編譯的時後會報錯，提示 "chisel3.package$ExpectedChiselTypeException: vec type 'UInt<1>(0)' must be a Chisel type, not hardware"
>
>正確的寫法應改成如下
>
>```scala=
>io.puzzle := VecInit(Seq.fill(4)(0.U(4.W)))
>```
>
>應該利用 `VecInit` 來初始化，而非直接用 `Vec`
>`Vec` 應作為宣告訊號線時 (without initialization) 的用法，如果要賦值則該採用 `VecInit`
:::

#### Introduction
- 作業要求：
    1. 請同學實作出一16 bits的LFSR，tap的數量以及擺放位置如下圖所示。
      ![](https://course.playlab.tw/md/uploads/c6d83b32-cc6e-4541-bf24-fa93df057358.png)

    2. 作為1A2B的隨機數字產生器，以4 bits為一個隨機的數字(共4個)。需額外考慮...
        1. 隨機數字範圍為10~15時該如何處理?
            - 將此範圍的數字依順序mapping至0~5
                - 以上圖為例：A C E 1 -> 0 2 4 1
        - 數字重複時該如何處理？
            - 請同學自行處理，並在說明文件中詳細闡述你所使用的方式。
  
- port declaration
    ```scala=
    class PRNG(seed:Int) extends Module{
        val io = IO(new Bundle{
            val gen = Input(Bool())
            val puzzle = Output(Vec(4,UInt(4.W)))
            val ready = Output(Bool())
        })
    }
    ```
    - `gen` : 觸發信號，當訊號為High時(時長為 1 clk)，module必須取樣LFSR的值，並處理成合理的**題目**
        :::danger 
        同學必須解決**超過範圍**、**重複值**這些問題
        :::
    - `puzzle`: 出題題目，為4*4bits格式，代表每個隨機數，index由左至右依序為3 2 1 0。
    - `ready` : 當題目處理好時，拉高至High，讓**外部**使用者知道題目已經準備好了。
- tester驗證方式
    - seed預設為1，同學可不必在意。
    - tester會在**隨機時刻**拉起gen訊號10次。
        - 每一次都會等到題目產生(ready至High時)才會發出下一次的gen訊號。
    - 檢測內容：
        1. 數字不超過範圍(0~9)
        2. 數字不重複
        3. 每次題目不重複。
- 執行指令...
    ```shell=
    $ sbt 'Test/runMain acal_lab05.Hw3.PRNGTest'
    ```

### Hw5-3-2 1A2B game quiz
#### Introduction
- 接續5-2-1的題目產生器，5-2-2需要同學能透過題目(puzzle)和猜測(guess)兩個訊號源實現一比較電路，並輸出比較結果(幾A幾B)。
- 電路行為如下說明：
    - tester會做的事：
        - tester會在隨機時刻拉高`gen`，prng會sample並處理好題目。
        - prng產生好題目時，ready訊號會被拉高。此時tester會要求同學由左至右輸入猜測內容。
    - 同學需完成的事：
        - 比較兩者內容，輸出比較結果:幾A幾B
        - 持續輸入猜測內容直至得到4A (放心一開始會印出題目...)
- 驗證流程演示：
    - from teminal
        ![](https://course.playlab.tw/md/uploads/73f3790f-8ac5-475f-a6ef-0b1eece44a3d.png =40%x)
    - from vcd
        ![](https://course.playlab.tw/md/uploads/ec55462d-5000-45c4-bf86-82df04749c0d.png)

- port declaration
```scala=
class NumGuess(seed:Int = 1) extends Module{
    require (seed > 0 , "Seed cannot be 0")
    
    val io  = IO(new Bundle{
        val gen = Input(Bool())
        val guess = Input(UInt(16.W))
        val puzzle = Output(Vec(4,UInt(4.W))) //for tester to print value...
        val ready  = Output(Bool())
        val valid  = Output(Bool())
        val A      = Output(UInt(3.W))
        val B      = Output(UInt(3.W))
    })
}
```
- 執行指令...
    ```shell=
    $ sbt 'Test/runMain acal_lab05.Hw3.NumGuessTest'
    ```
### Bonus : 1A2B hardware solver
#### Introduction
![](https://course.playlab.tw/md/uploads/36e6009e-9fa3-4af4-9dec-582cdd1f6063.png)

1. 將自已的解題技巧implement至硬體上，完成solver.scala。
    - 猜測數字可以重複，ex:1 1 1 1 
    - 在文件中描述自己的**解題策略**
- port declaration
    ```scala=
    class Solver extends Module{
        val io = IO(new Bundle{
            val A = Input(UInt(3.W))
            val B = Input(UInt(3.W))
            val ready = Input(Bool())
            val guess = Output(Vec(4,UInt(4.W)))
            val g_valid = Input(Bool())
            val s_valid = Output(Bool())
            val finish = Output(Bool()) //end signal
        })
    }
    ```
2. 利用top.scala將兩個Module(NumGuess.scala和Solver.scala)包起來，提供兩個模塊的互動環境。
    - 兩個module需互動直至A=4，`finish`=High
    - waveview for reference
        ![](https://course.playlab.tw/md/uploads/1bb8b475-d443-41cc-b8ba-21e72f70ec7d.png)
- port declaration and wiring
    ```scala=
    class top extends Module{
        val io  = IO(new Bundle{
            val gen = Input(Bool())
            val finish = Output(Bool())
        })

        val ng = Module(new NumGuess(1))
        val ns = Module(new Solver())

        ng.io.gen := io.gen
        ng.io.guess := ns.io.guess.asUInt
        ng.io.s_valid := ns.io.s_valid

        ns.io.A := ng.io.A
        ns.io.B := ng.io.B
        ns.io.ready := ng.io.ready
        ns.io.g_valid := ng.io.g_valid

        io.finish := ns.io.finish
    }
    ```
## Homework Submission Rule
- **Step 1**
    - 請在自己的 GitLab內建立 `lab05` repo，並將本次 Lab 撰寫的程式碼放入這個repo。另外記得開權限給助教還有老師。
- **Step 2**
    - 請參考 [(校名_學號_姓名) ACAL 2024 Spring Lab 5 HW Submission Template](https://course.playlab.tw/md/SLvf5zJ8REOZyhuJAYLb_w)，建立(複製一份)並自行撰寫 CodiMD 作業說明文件。請勿更動template裡的內容。
    - 關於 gitlab 開權限給助教群組的方式可以參照以下連結
        - [ACAL 2024 Curriculum GitLab 作業繳交方式說明 : Manage Permission](https://course.playlab.tw/md/CW_gy1XAR1GDPgo8KrkLgg#Manage-Permission)
- **Step 3**
    - When you are done, please submit your homework document link to the Playlab 作業中心, <font style="color:blue"> 清華大學與陽明交通大學的同學請注意選擇對的作業中心鏈結</font>
        - [清華大學Playlab 作業中心](https://nthu-homework.playlab.tw/course?id=2)
        - [陽明交通大學作業繳交中心](https://course.playlab.tw/homework/course?id=2)

    
    
