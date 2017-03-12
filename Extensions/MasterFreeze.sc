MasterFreeze {

	var bStart, sGain, tOutput, sLevelMainBus, synthGroup, processTask, countTask, tName;

	*new
	{
		arg 	name, 			// name of the instance
		buf, 				// buffer
		waitArray = [10,10,20], 		// array with wait times between freezes (seconds); this also sets the total length
		openArray, 		// array with open times of the freeze gate (seconds)
		dieOutArray, 		// array with dieOut values to be sent with every freeze
		filterAttack = 0.5, 	// attack for LP filter in freezeType 1, 2 & 3 (seconds)
		filterFade = 30, 	// release for LP filter in freeze Type 1, 2 & 3 (seconds)
		initWait = 0,		// initial wait before freeze kicks in (seconds)
		endWait = 10,		// extra time at the end to allow the processing to develop (seconds)
		startPos = 2,		// startposition for playback sample (seconds)
		output = [0,1],		// output channel, stereo
		subOut = 8,		// output channel for sub
		countDisp = 1, 		// display count, 0=NO 1=YES
		freezeType = 1, 	// freeze function type, 1=short delay continous 2=longer delay fading in/out, 3=repeats, 4=pulse
		pitchFactor = 1,		// multiplication factor for pitch
		gainFactor = -3,	// multiplication factor for volume (dB)
		guiIndex = 1,		// index for GUI placement
		guiVisible = 1,		// 0=hide all GUI
		guiTop = 20,		// vertical offsett for GUI elements
		win;				// parent window for GUI

		^super.new.initMasterFreeze(name, buf, waitArray, openArray, dieOutArray, filterAttack, filterFade, initWait, endWait, startPos,
			output, subOut, countDisp, freezeType, pitchFactor, gainFactor, guiIndex, guiVisible, guiTop, win);
	}

	initMasterFreeze
	{
		arg name, buf, waitArray, openArray, dieOutArray, filterAttack, filterFade, initWait, endWait, startPos, output, subOut, countDisp,
		freezeType, pitchFactor, gainFactor, guiIndex, guiVisible, guiTop, win;

		// ------- variables -------------------------------------------------------------

		var server = Server.default;
		var subGain = 0.5;

		var detailColor = Color.new255(100,100,155);
		var backgroundColor = Color.grey(0.9);
		var penColorYellow = Color.new255(231,185,106, rrand(100,255));

		var processFunc, countFunc, freezeSynth, patt;
		var pFreezeType, nPitch, bMute, lOutput, bMuteBus = Bus.control(server,1), sGainBus = Bus.control(server,1);
		var displayPrefix = "", trackLength = 0, localWin = 0, screenHeight = Window.screenBounds.height;
		var drawDone = 0, backDrop;
		var fBufferFreeze1, fBufferFreeze2, fBufferFreeze3, fBufferFreeze4;

		synthGroup = Group.new();
		sLevelMainBus = Bus.control(server,1);

		// populate openArray and dieOutArray if not provided
		if(openArray.isNil, {
			openArray = Array.new(waitArray.size);
			waitArray.size.do({
				openArray.add(rrand(0.1,0.4));
			});
			// openArray.postln;
		});
		if(dieOutArray.isNil, {
			dieOutArray = Array.new(waitArray.size);
			waitArray.size.do({
				dieOutArray.add(0.7);
			});
			// dieOutArray.postln;
		});

		// calculate trackLength
		waitArray.size.do({|i|
			trackLength = trackLength + waitArray[i] + openArray[i];
		});
		trackLength = trackLength + initWait + 10 + endWait;
		("Track" + name + "length:" + trackLength + "seconds").postln;

		// if no win provided, create local window
		if(win.isNil, {localWin = 1});


		// ------- functions for SynthDefs -------------------------------------------------------------

		// -------------------------------- Freeze1 -------------------------------------------------------------

		fBufferFreeze1 = {
			arg feedback, hold, gate, pitch, gainBus, start, muteBus, openfilter, volMainBus, buf, filterFade, filterAttack, sRate;
			var in, localL, localR, sig, local, envLPF;

			// Rand is different everytime the Synth starts
			//		SendTrig.kr(Impulse.kr(4),1,Rand(0,10));

			// playback sample
			in = BRF.ar(PlayBuf.ar(2, buf, BufRateScale.kr(buf)*pitch, startPos: sRate*start, loop: 1),110,0.5);

			// feedback sample
			local = LocalIn.ar(2);
			localL = (max(hold,0.1) * local[0]) + (in[0] * (1 - hold)); // switch input feedback loop between in and local
			localR = (max(hold,0.1) * local[1]) + (in[1] * (1 - hold));
			20.do{
				localL = AllpassC.ar(localL,0.05,Rand(0.001,0.05),Rand(1,3));
				localR = AllpassC.ar(localR,0.05,Rand(0.001,0.05),Rand(1,3));
			};
			LocalOut.ar([localL,localR]*feedback);

			//		SendTrig.kr(Impulse.kr(4),1,filterFade);

			// set LPF filter
			envLPF = EnvGen.ar(Env.linen(filterAttack,0,filterFade,1,\lin).range(500,15000), gate: openfilter, doneAction: 0);
			envLPF = envLPF + LFNoise1.ar(0.2).range(-300,1000);

			// sig processing to send out
			sig = [localL,localR] * EnvGen.kr(Env.adsr(0.01,0,1,5), gate, doneAction: 2);
			sig = HPF.ar(sig, 50);
			sig = LPF.ar(sig, envLPF);
			sig = sig * In.kr(gainBus,1) * (1-In.kr(muteBus,1));
			sig = sig * In.kr(volMainBus,1);
			sig // pass as output
		};

		// -------------------------------- Freeze2 (fade in/out variation) -------------------------------------------------------------

		fBufferFreeze2 = {
			arg feedback, hold, gate, dieOut, pitch, gainBus, start, muteBus, openfilter, volMainBus, buf, filterFade, filterAttack, sRate;
			var in, localL, localR, sig, local, envLPF;

			// playback sample
			in = BRF.ar(PlayBuf.ar(2, buf, BufRateScale.kr(buf)*pitch, startPos: sRate*start, loop: 1),110,0.5);

			// feedback sample
			local = LocalIn.ar(2);
			localL = (max(hold,0.1) * local[0]) + (in[0] * (1 - hold));
			localR = (max(hold,0.1) * local[1]) + (in[1] * (1 - hold));
			20.do{
				localL = AllpassC.ar(localL,1,Rand(0.1,1),Rand(3,5));
				localR = AllpassC.ar(localR,1,Rand(0.1,1),Rand(3,5));
			};
			LocalOut.ar([
				localL*SinOsc.ar(LFNoise1.ar(1).range(0.03,0.1)).range(dieOut ,1.1), // amplify pulse in fade
				localR*SinOsc.ar(LFNoise1.ar(1).range(0.03,0.1)).range(dieOut,1.2)
			]*feedback);

			//		SendTrig.kr(Impulse.kr(4),1,filterAttack);

			// set LPF filter
			envLPF = EnvGen.ar(Env.linen(filterAttack,0,filterFade,1,\lin).range(500,15000), gate: openfilter, doneAction: 0);
			envLPF = envLPF + LFNoise1.ar(0.2).range(-300,1000);

			// sig processing to send out
			sig = [localL,localR] * EnvGen.kr(Env.adsr(0.01,0,1,5), gate, doneAction: 2);
			sig = HPF.ar(sig, 50);
			sig = LPF.ar(sig, envLPF);
			sig = sig * In.kr(gainBus,1) * (1-In.kr(muteBus,1));
			sig = sig * In.kr(volMainBus,1);
			sig // pass as output
		};

		// -------------------------------- Freeze3 (clear repeats variation) -------------------------------------------------------------

		fBufferFreeze3 = {
			arg feedback, hold, gate, pitch, gainBus, start, muteBus, openfilter, volMainBus, buf, filterFade, filterAttack, sRate;
			var in, localL, localR, sig, local, envLPF;

			// playback sample
			in = BRF.ar(PlayBuf.ar(2, buf, BufRateScale.kr(buf)*pitch, startPos: sRate*start, loop: 1),110,0.5);

			// feedback sample
			local = LocalIn.ar(2);
			localL = (max(hold,0.1) * local[0]) + (in[0] * (1 - hold));
			localR = (max(hold,0.1) * local[1]) + (in[1] * (1 - hold));
			1.do{
				localL = AllpassC.ar(localL,5,Rand(0.15,0.8),3); // max delay was 5
				localR = AllpassC.ar(localR,4,Rand(0.1,0.7),3); // max delay was 4
			};
			LocalOut.ar([localL,localR]*feedback);

			// set LPF filter
			envLPF = EnvGen.ar(Env.linen(filterAttack,0,filterFade,1,\lin).range(500,15000), gate: openfilter, doneAction: 0);
			envLPF = envLPF + LFNoise1.ar(0.2).range(-300,500);
			//		envLPF = LFNoise1.ar(0.2).range(300,2000);

			// sig processing to send out
			sig = [localL,localR] * EnvGen.kr(Env.adsr(0.01,0,1,5), gate, doneAction: 2);
			sig = HPF.ar(sig, 50);
			sig = LPF.ar(sig, envLPF);
			sig = sig * In.kr(gainBus,1) * (1-In.kr(muteBus,1));
			sig = sig * In.kr(volMainBus,1);
			sig // pass as output
		};

		// -------------------------------- Freeze4 (pulse variation of bufferFreeze1) -------------------------------------------------------------

		fBufferFreeze4 = {
			arg feedback, hold, gate, pitch, gainBus, start, muteBus, volMainBus, buf, filterFade, filterAttack, sRate;
			var in, localL, localR, sig, local, envLPF, pulse, pulseTrig, pulseLen;

			// playback sample
			in = BRF.ar(PlayBuf.ar(2, buf, BufRateScale.kr(buf)*pitch, startPos: sRate*start, loop: 1),110,0.5);

			// feedback sample
			local = LocalIn.ar(2);
			localL = (max(hold,0.1) * local[0]) + (in[0] * (1 - hold)); // switch input feedback loop between in and local
			localR = (max(hold,0.1) * local[1]) + (in[1] * (1 - hold));
			20.do{
				localL = AllpassC.ar(localL,0.05,Rand(0.001,0.05),Rand(1,3));
				localR = AllpassC.ar(localR,0.05,Rand(0.001,0.05),Rand(1,3));
			};
			LocalOut.ar([localL,localR]*feedback);

			// set LPF filter
			envLPF = LFNoise1.ar(0.2).range(300,1000);

			// sinusoid pulse
			//		pulse = SinOsc.ar(SinOsc.ar(0.02).range(1.1,1.2)).range(0.2,1);

			// percussive pulse
			pulseLen = SinOsc.ar(0.02).range(1.1,1.25);
			pulseTrig = Impulse.ar(pulseLen);
			pulse = EnvGen.ar(Env.linen(0.02,0,pulseLen-0.1,1,\lin).range(0.2,1), gate: pulseTrig, doneAction: 0);

			// sig processing to send out
			sig = [localL,localR] * EnvGen.kr(Env.adsr(0.01,0,1,5), gate, doneAction: 2);
			sig = sig * pulse * SinOsc.ar(0.1).range(0.7,1.5); // add short term and long term pulse
			sig = HPF.ar(sig, 50);
			sig = LPF.ar(sig, envLPF);
			sig = sig * In.kr(gainBus,1) * (1-In.kr(muteBus,1));
			sig = sig * In.kr(volMainBus,1);
			sig // pass as output
		};


		// ------- SynthDefs -------------------------------------------------------------

		// -------------------------------- Freeze1 -------------------------------------------------------------

		SynthDef(\bufferFreeze1_2ch ,{
			arg feedback = 0.5, hold = 0, gate, out1, out2, pitch, gainBus, start, muteBus, openfilter = 0, volMainBus, sub, buf,
			filterFade, filterAttack, sRate;
			var sig = fBufferFreeze1.value(feedback, hold, gate, pitch, gainBus, start, muteBus, openfilter, volMainBus, buf, filterFade,
				filterAttack, sRate);
			Out.ar(out1, sig[0]); Out.ar(out2, sig[1]);
			Out.ar(sub, subGain*Mix.new(sig));
		}).add();

		SynthDef(\bufferFreeze1_4ch ,{
			arg feedback = 0.5, hold = 0, gate, out1, out2, out3, out4, pitch, gainBus, start, muteBus,
			openfilter = 0, volMainBus, sub, buf, filterFade, filterAttack, sRate;
			var sig = fBufferFreeze1.value(feedback, hold, gate, pitch, gainBus, start, muteBus, openfilter, volMainBus, buf, filterFade,
				filterAttack, sRate);
			Out.ar(out1, sig[0]); Out.ar(out2, sig[1]); Out.ar(out3, sig[0]); Out.ar(out4, sig[1]);
			Out.ar(sub, subGain*Mix.new(sig));
		}).add();

		SynthDef(\bufferFreeze1_6ch ,{
			arg feedback = 0.5, hold = 0, gate, out1, out2, out3, out4, out5, out6, pitch, gainBus, start, muteBus,
			openfilter = 0, volMainBus, sub, buf, filterFade, filterAttack, sRate;
			var sig = fBufferFreeze1.value(feedback, hold, gate, pitch, gainBus, start, muteBus, openfilter, volMainBus, buf, filterFade,
				filterAttack, sRate);
			Out.ar(out1, sig[0]); Out.ar(out2, sig[1]); Out.ar(out3, sig[0]); Out.ar(out4, sig[1]); Out.ar(out5, sig[0]); Out.ar(out6, sig[1]);
			Out.ar(sub, subGain*Mix.new(sig));
		}).add();

		SynthDef(\bufferFreeze1_8ch ,{
			arg feedback = 0.5, hold = 0, gate, out1, out2, out3, out4, out5, out6, out7, out8, pitch, gainBus, start,
			muteBus, openfilter = 0, volMainBus, sub, buf, filterFade, filterAttack, sRate;
			var sig = fBufferFreeze1.value(feedback, hold, gate, pitch, gainBus, start, muteBus, openfilter, volMainBus, buf, filterFade,
				filterAttack, sRate);
			Out.ar(out1, sig[0]); Out.ar(out2, sig[1]); Out.ar(out3, sig[0]); Out.ar(out4, sig[1]);
			Out.ar(out5, sig[0]); Out.ar(out6, sig[1]); Out.ar(out7, sig[0]); Out.ar(out8, sig[1]);
			Out.ar(sub, subGain*Mix.new(sig));
		}).add();

		// -------------------------------- Freeze2 (fade in/out variation) -------------------------------------------------------------

		SynthDef(\bufferFreeze2_2ch ,{ // fade in/out variation
			arg feedback = 0.5, hold = 0, gate, dieOut = 0.7, out1, out2, pitch, gainBus, start, muteBus, openfilter = 0,
			volMainBus, sub, buf, filterFade, filterAttack, sRate;
			var sig = fBufferFreeze2.value(feedback, hold, gate, dieOut, pitch, gainBus, start, muteBus, openfilter, volMainBus, buf,
				filterFade, filterAttack, sRate);
			Out.ar(out1, sig[0]); Out.ar(out2, sig[1]);
			Out.ar(sub, subGain*Mix.new(sig));
		}).add();

		SynthDef(\bufferFreeze2_4ch ,{ // fade in/out variation
			arg feedback = 0.5, hold = 0, gate, dieOut = 0.7, out1, out2, out3, out4, pitch, gainBus, start, muteBus,
			openfilter = 0, volMainBus, sub, buf, filterFade, filterAttack, sRate;
			var sig = fBufferFreeze2.value(feedback, hold, gate, dieOut, pitch, gainBus, start, muteBus, openfilter, volMainBus, buf,
				filterFade, filterAttack, sRate);
			Out.ar(out1, sig[0]); Out.ar(out2, sig[1]); Out.ar(out3, sig[0]); Out.ar(out4, sig[1]);
			Out.ar(sub, subGain*Mix.new(sig));
		}).add();

		SynthDef(\bufferFreeze2_6ch ,{ // fade in/out variation
			arg feedback = 0.5, hold = 0, gate, dieOut = 0.7, out1, out2, out3, out4, out5, out6, pitch, gainBus, start,
			muteBus, openfilter = 0, volMainBus, sub, buf, filterFade, filterAttack, sRate;
			var sig = fBufferFreeze2.value(feedback, hold, gate, dieOut, pitch, gainBus, start, muteBus, openfilter, volMainBus, buf,
				filterFade, filterAttack, sRate);
			Out.ar(out1, sig[0]); Out.ar(out2, sig[1]); Out.ar(out3, sig[0]); Out.ar(out4, sig[1]); Out.ar(out5, sig[0]); Out.ar(out6, sig[1]);
			Out.ar(sub, subGain*Mix.new(sig));
		}).add();

		SynthDef(\bufferFreeze2_8ch ,{ // fade in/out variation
			arg feedback = 0.5, hold = 0, gate, dieOut = 0.7, out1, out2, out3, out4, out5, out6, out7, out8, pitch, gainBus, start,
			muteBus, openfilter = 0, volMainBus, sub, buf, filterFade, filterAttack, sRate;
			var sig = fBufferFreeze2.value(feedback, hold, gate, dieOut, pitch, gainBus, start, muteBus, openfilter, volMainBus, buf,
				filterFade, filterAttack, sRate);
			Out.ar(out1, sig[0]); Out.ar(out2, sig[1]); Out.ar(out3, sig[0]); Out.ar(out4, sig[1]);
			Out.ar(out5, sig[0]); Out.ar(out6, sig[1]); Out.ar(out7, sig[0]); Out.ar(out8, sig[1]);
			Out.ar(sub, subGain*Mix.new(sig));
		}).add();

		// -------------------------------- Freeze3 (clear repeats variation) -------------------------------------------------------------

		SynthDef(\bufferFreeze3_2ch ,{ // clear repeats variation
			arg feedback = 0.5, hold = 0, gate, out1, out2, pitch, gainBus, start, muteBus, openfilter = 0,
			volMainBus, sub, buf, filterFade, filterAttack, sRate;
			var sig = fBufferFreeze3.value(feedback, hold, gate, pitch, gainBus, start, muteBus, openfilter, volMainBus, buf,
				filterFade, filterAttack, sRate);
			Out.ar(out1, sig[0]); Out.ar(out2, sig[1]);
			Out.ar(sub, subGain*Mix.new(sig));
		}).add();

		SynthDef(\bufferFreeze3_4ch ,{ // clear repeats variation
			arg feedback = 0.5, hold = 0, gate, out1, out2, out3, out4, pitch, gainBus, start, muteBus,
			openfilter = 0, volMainBus, sub, buf, filterFade, filterAttack, sRate;
			var sig = fBufferFreeze3.value(feedback, hold, gate, pitch, gainBus, start, muteBus, openfilter, volMainBus, buf,
				filterFade, filterAttack, sRate);
			Out.ar(out1, sig[0]); Out.ar(out2, sig[1]); Out.ar(out3, sig[0]); Out.ar(out4, sig[1]);
			Out.ar(sub, subGain*Mix.new(sig));
		}).add();

		SynthDef(\bufferFreeze3_6ch ,{ // clear repeats variation
			arg feedback = 0.5, hold = 0, gate, out1, out2, out3, out4, out5, out6, pitch, gainBus, start, muteBus,
			openfilter = 0, volMainBus, sub, buf, filterFade, filterAttack, sRate;
			var sig = fBufferFreeze3.value(feedback, hold, gate, pitch, gainBus, start, muteBus, openfilter, volMainBus, buf,
				filterFade, filterAttack, sRate);
			Out.ar(out1, sig[0]); Out.ar(out2, sig[1]); Out.ar(out3, sig[0]); Out.ar(out4, sig[1]); Out.ar(out5, sig[0]); Out.ar(out6, sig[1]);
			Out.ar(sub, subGain*Mix.new(sig));
		}).add();

		SynthDef(\bufferFreeze3_8ch ,{ // clear repeats variation
			arg feedback = 0.5, hold = 0, gate, out1, out2, out3, out4, out5, out6, out7, out8, pitch, gainBus, start,
			muteBus, openfilter = 0, volMainBus, sub, buf, filterFade, filterAttack, sRate;
			var sig = fBufferFreeze3.value(feedback, hold, gate, pitch, gainBus, start, muteBus, openfilter, volMainBus, buf,
				filterFade, filterAttack, sRate);
			Out.ar(out1, sig[0]); Out.ar(out2, sig[1]); Out.ar(out3, sig[0]); Out.ar(out4, sig[1]);
			Out.ar(out5, sig[0]); Out.ar(out6, sig[1]); Out.ar(out7, sig[0]); Out.ar(out8, sig[1]);
			Out.ar(sub, subGain*Mix.new(sig));
		}).add();

		// -------------------------------- Freeze4 (pulse variation of bufferFreeze1) -------------------------------------------------------------

		SynthDef(\bufferFreeze4_2ch ,{ // pulse variation of bufferFreeze1
			arg feedback = 0.5, hold = 0, gate, out1, out2, pitch, gainBus, start, muteBus, volMainBus, sub, buf, filterFade, filterAttack, sRate;
			var sig = fBufferFreeze4.value(feedback, hold, gate, pitch, gainBus, start, muteBus, volMainBus, buf, filterFade, filterAttack,
				sRate);
			Out.ar(out1, sig[0]); Out.ar(out2, sig[1]);
			Out.ar(sub, subGain*Mix.new(sig));
		}).add();

		SynthDef(\bufferFreeze4_4ch ,{ // pulse variation of bufferFreeze1
			arg feedback = 0.5, hold = 0, gate, out1, out2, out3, out4, pitch, gainBus, start, muteBus, volMainBus, sub, buf, filterFade,
			filterAttack, sRate;
			var sig = fBufferFreeze4.value(feedback, hold, gate, pitch, gainBus, start, muteBus, volMainBus, buf, filterFade, filterAttack,
				sRate);
			Out.ar(out1, sig[0]); Out.ar(out2, sig[1]); Out.ar(out3, sig[0]); Out.ar(out4, sig[1]);
			Out.ar(sub, subGain*Mix.new(sig));
		}).add();

		SynthDef(\bufferFreeze4_6ch ,{ // pulse variation of bufferFreeze1
			arg feedback = 0.5, hold = 0, gate, out1, out2, out3, out4, out5, out6, pitch, gainBus, start, muteBus, volMainBus, sub, buf,
			filterFade, filterAttack, sRate;
			var sig = fBufferFreeze4.value(feedback, hold, gate, pitch, gainBus, start, muteBus, volMainBus, buf, filterFade, filterAttack,
				sRate);
			Out.ar(out1, sig[0]); Out.ar(out2, sig[1]); Out.ar(out3, sig[0]); Out.ar(out4, sig[1]); Out.ar(out5, sig[0]); Out.ar(out6, sig[1]);
			Out.ar(sub, subGain*Mix.new(sig));
		}).add();

		SynthDef(\bufferFreeze4_8ch ,{ // pulse variation of bufferFreeze1
			arg feedback = 0.5, hold = 0, gate, out1, out2, out3, out4, out5, out6, out7, out8, pitch, gainBus, start, muteBus, volMainBus, sub,
			buf, filterFade, filterAttack, sRate;
			var sig = fBufferFreeze4.value(feedback, hold, gate, pitch, gainBus, start, muteBus, volMainBus, buf, filterFade, filterAttack,
				sRate);
			Out.ar(out1, sig[0]); Out.ar(out2, sig[1]); Out.ar(out3, sig[0]); Out.ar(out4, sig[1]);
			Out.ar(out5, sig[0]); Out.ar(out6, sig[1]); Out.ar(out7, sig[0]); Out.ar(out8, sig[1]);
			Out.ar(sub, subGain*Mix.new(sig));
		}).add();


		// ------- functions -------------------------------------------------------------

		countFunc = { // this function starts the synths, starts the processing task and does the counting
			// starting synth
			("Track" + name ++ ":" + output.size + "channels").postln;
			case
			{output.size == 2}
			{
				freezeSynth = Synth(\bufferFreeze++freezeType++"_2ch", [
					\feedback,0.5,\hold,1,\gate,1,
					\out1, output[0].asInt,\out2, output[1].asInt,
					\pitch, pitchFactor, \start, startPos,
					\muteBus, bMuteBus.index, \gainBus, sGainBus.index, \openfilter, 1,
					\volMainBus, sLevelMainBus.index, \sub, subOut,
					\buf, buf, \filterFade, filterFade, \filterAttack, filterAttack,
					\sRate, server.sampleRate
				], target: synthGroup);
			}
			{output.size == 4}
			{
				freezeSynth = Synth(\bufferFreeze++freezeType++"_4ch", [
					\feedback,0.5,\hold,1,\gate,1,
					\out1, output[0].asInt,\out2, output[1].asInt,\out3, output[2].asInt,\out4, output[3].asInt,
					\pitch,pitchFactor, \start,startPos,
					\muteBus,bMuteBus.index, \gainBus,sGainBus.index, \openfilter, 1,
					\volMainBus, sLevelMainBus.index, \sub, subOut,
					\buf, buf, \filterFade, filterFade, \filterAttack, filterAttack,
					\sRate, server.sampleRate
				], target: synthGroup);
			}
			{output.size == 6}
			{
				freezeSynth = Synth(\bufferFreeze++freezeType++"_6ch", [
					\feedback,0.5,\hold,1,\gate,1,
					\out1, output[0].asInt,\out2, output[1].asInt,\out3, output[2].asInt,\out4, output[3].asInt,
					\out5, output[4].asInt,\out6, output[5].asInt,
					\pitch,pitchFactor, \start,startPos,
					\muteBus,bMuteBus.index, \gainBus,sGainBus.index, \openfilter, 1,
					\volMainBus, sLevelMainBus.index, \sub, subOut,
					\buf, buf, \filterFade, filterFade, \filterAttack, filterAttack,
					\sRate, server.sampleRate
				], target: synthGroup);
			}
			{output.size == 8}
			{
				freezeSynth = Synth(\bufferFreeze++freezeType++"_8ch", [
					\feedback,0.5,\hold,1,\gate,1,
					\out1, output[0].asInt,\out2, output[1].asInt,\out3, output[2].asInt,\out4, output[3].asInt,
					\out5, output[4].asInt,\out6, output[5].asInt,\out7, output[6].asInt,\out8, output[7].asInt,
					\pitch,pitchFactor, \start,startPos,
					\muteBus,bMuteBus.index, \gainBus,sGainBus.index, \openfilter, 1,
					\volMainBus, sLevelMainBus.index, \sub, subOut,
					\buf, buf, \filterFade, filterFade, \filterAttack, filterAttack,
					\sRate, server.sampleRate
				], target: synthGroup);
			}
			;

			// starting processing
			{freezeSynth.set(\hold,0)}.defer(0.1);
			{processTask = Task(processFunc).play}.defer(initWait + 0.5.rand);

			// count
			trackLength.do({
				arg i;
				if(countDisp==1, {(displayPrefix ++ name + (i.asTimeStringHM)).postln});
				1.wait;
			});

			// stop all
			{if(bStart.value == 1, {bStart.valueAction_(0); bStart.enabled = true;})}.defer();

		};

		processFunc = { // this function does all the processing after the Synth is started - so this is the main structure
			freezeSynth.set(\hold,1);
			freezeSynth.set(\openfilter,1);
			freezeSynth.set(\feedback, 1);

			waitArray.size.do({|i|
				(rrand(waitArray[i]-5,waitArray[i]+5)).wait;
				freezeSynth.set(\openfilter,0); 0.1.wait; freezeSynth.set(\openfilter,1);
				(displayPrefix ++ name + ">>>>> OPEN FREEZE" + i ++ " >>>>>").postln;
				freezeSynth.set(\hold,0);
				freezeSynth.set(\feedback, 0);
				rrand(openArray[i]-0.1,openArray[i]+0.1).wait;
				(displayPrefix ++ name + ">>>>> CLOSE FREEZE" + i ++ " >>>>>").postln;
				freezeSynth.set(\hold,1);
				freezeSynth.set(\feedback, 1);
				freezeSynth.set(\dieOut, dieOutArray[i]);
			});
		};


		// ------- GUI -------------------------------------------------------------

		// create window if there's no parent
		if(localWin == 1, {
			win = Window("MasterFreeze Track" + name, Rect(20,screenHeight-(guiIndex*140)-30,600,guiTop+80));
			win.alpha_(0.9);
			win.background_(backgroundColor);
			win.onClose_({
				bStart.valueAction_(0);
			});

			backDrop = UserView(win,Rect(0,0,600,600));
			backDrop.drawFunc = {
				if(drawDone == 0, {
					rrand(40,200).do{|i|
						Pen.color = penColorYellow;
						Pen.addRect(
							Rect((win.bounds.width).rand, -2, 20, (win.bounds.height + 4))
						);
						Pen.perform(\stroke);
					};
					drawDone = 1;
				})
			};
			backDrop.clearOnRefresh_(false);
		});

		tName = StaticText(win, Rect(75, guiTop+((guiIndex-1)*80*(1-localWin))+2, 225, 15))
		.font_(Font("Helvetica",9))
		.string_(" "++name)
		.background_(Color.white);

		sGain = EZSlider(win, Rect(30, guiTop+((guiIndex-1)*80*(1-localWin)), 340, 18), "Gain  ",
			ControlSpec(0.ampdb, 12, \db, units: "dB", step: 0.01),
			numberWidth: 40, layout: \horz, unitWidth: 25, initVal: gainFactor, labelWidth: 40,
			action: {
				sGainBus.set(sGain.value.dbamp);
				// ("Track"+name+"gain set to"+sGain.value+"dB").postln;
		});
		sGain.setColors(detailColor,Color.white, Color.white.alpha_(0),Color.white, Color.black, Color.black, detailColor, detailColor);
		sGain.font_(Font("Helvetica",12));
		sGain.round_(0.01);

		lOutput = StaticText(win, Rect(380, guiTop+((guiIndex-1)*80*(1-localWin)), 60, 18))
		.font_(Font("Helvetica",12))
		.stringColor_(Color.white);
		lOutput.string = " OutChan";
		lOutput.background = detailColor;

		tOutput = TextField(win, Rect(442, guiTop+((guiIndex-1)*80*(1-localWin)), 80, 18));
		tOutput.font_(Font("Helvetica",12));
		tOutput.string = subStr(output.asString,2,output.asString.size-2).replace(" ","");
		tOutput.action = ({
			output = tOutput.value.split($,); // convert into array, which should have even length
			case
			{(output.size)%2 == 0} // even
			{
				("Track"+name+"output set to"+output ).postln;
			}
			{(output.size)%2 == 1} // odd
			{
				output = [0,1];
				tOutput.value = "0,1";
				("Track"+name+"error: the output array should have an even number of entries, comma separated, max 8").postln;
			}
			;
		});

		bStart = SmoothButton(win, Rect(530, guiTop+((guiIndex-1)*80*(1-localWin)), 40, 18))
		.radius_(2)
		.border_(1)
		.font_(Font("Helvetica",12));
		bStart.canFocus = false;
		bStart.states = [["Start",Color.black,Color.white],["Stop",Color.white,Color.black]];
		bStart.action = {
			if(bStart.value == 1, {
				("Start track" + name).postln;
				{countTask = Task(countFunc).play}.defer(0.5);

				tOutput.enabled = false;
				pFreezeType.enabled = false;
				nPitch.numberView.enabled = false;
				},
				{
					("Stop track" + name).postln;
					freezeSynth.set(\gate,0);
					countTask.stop;
					{processTask.stop}.defer(initWait+1);

					tOutput.enabled = true;
					pFreezeType.enabled = true;
					nPitch.numberView.enabled = true;
			});
		};

		pFreezeType = EZPopUpMenu(win, Rect(30, guiTop+30+((guiIndex-1)*80*(1-localWin)), 110, 18), "Type  ",
			[
				\freeze1 -> {arg a; freezeType = 1; ("Track"+name+"freezeType set to"+freezeType).postln;},
				\freeze2 -> {arg a; freezeType = 2; ("Track"+name+"freezeType set to"+freezeType).postln;},
				\freeze3 -> {arg a; freezeType = 3; ("Track"+name+"freezeType set to"+freezeType).postln;},
				\freeze4 -> {arg a; freezeType = 4; ("Track"+name+"freezeType set to"+freezeType).postln;}
			],
			initVal: freezeType - 1, labelWidth: 40);
		pFreezeType.font_(Font("Helvetica",12));
		pFreezeType.setColors(detailColor,Color.white, Color.white, Color.black);

		nPitch = EZNumber(win, Rect(150, guiTop+30+((guiIndex-1)*80*(1-localWin)), 110, 18), "Pitch factor  ",
			ControlSpec(0.2,10,step:0.01),
			action: {
				pitchFactor = nPitch.value;
				// ("Track"+name+"pitch factor set to"+pitchFactor).postln;
			},
			initVal: pitchFactor, labelWidth: 72);
		nPitch.font_(Font("Helvetica",12));
		nPitch.setColors(detailColor,Color.white, Color.white, Color.black, Color.black, detailColor,detailColor);

		bMute = SmoothButton(win, Rect(530, guiTop+30+((guiIndex-1)*80*(1-localWin)), 40, 18))
		.radius_(2)
		.border_(1)
		.font_(Font("Helvetica",12));
		bMute.canFocus = false;
		bMute.states = [["Mute",Color.black,Color.white],["Mute",Color.white,Color.black]];
		bMute.action = {bMuteBus.set(bMute.value)};

		// if local window, show it
		if(localWin == 1, {
			win.front;
		});

		// ------- Initialize -------------------------------------------------------------

		sLevelMainBus.set(1);
		bMuteBus.set(0);
		sGainBus.set(gainFactor.dbamp);

		// prefix for postln information display
		(guiIndex-1).do({
			displayPrefix = displayPrefix + "             ";
		});

		if(guiVisible==0, {
			sGain.visible_(false);
			bStart.visible_(false);
			tOutput.visible_(false);
			lOutput.visible_(false);
			bMute.visible_(false);
			nPitch.visible_(false);
			pFreezeType.visible_(false);
		});

	}

	setOutput
	{
		arg out;
		tOutput.valueAction_(out)
	}

	start
	{
		{if(bStart.value == 0, {bStart.valueAction_(1); bStart.enabled = false;})}.defer();
		//	rrand is different everytime this function is called
		//	rrand(1,10).postln;
	}

	stop
	{
		arg forced = 0;
		case
		{forced == 0}
		{
			{if(bStart.value == 1, {bStart.valueAction_(0); bStart.enabled = true;})}.defer();
		}
		{forced == 1}
		{
			processTask.stop;
			countTask.stop;
			synthGroup.free;
		}
	}

	levelRamp
	{
		arg endLevel, releaseTime; // dB & seconds
		var currentLevel = sGain.value;

		var step = (currentLevel-endLevel)/(releaseTime*10);
		var levelTask;

		levelTask = Task.new({
			((releaseTime*10)+1).do({|i|
				{sGain.valueAction_(
					(currentLevel-(step*i))
				)}.defer();
				0.1.wait;
			});
		});
		levelTask.play;
	}

	levelMain
	{
		arg mainLevel; // dB
		sLevelMainBus.set(mainLevel.dbamp);
	}

}