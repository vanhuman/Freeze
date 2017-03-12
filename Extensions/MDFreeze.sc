MDFreeze {

var bStart, tOutput, sLevel, sLevelMainBus;

*new
	{
	arg 	name, 			// name of the instance
		index, 			// index of instance (i.e. how many have gone before)
		buf, 				// buffer
		version = 1,		// 1=long 10:00 2=short 5:30 (short version is basically the first half of the long version) 
		lengthFactor = 1, 	// multiplication factor for time lengths, including the overall lenght as set by the version argument
		output = [0,1],		// output channel, stereo
		countDisp = 0, 		// display count, 0=NO 1=YES
		envCurve = 0, 		// envelope curve for LP filter, 0=lin 1=exp
		freezeType = 1, 	// freeze function type, 1=short delay continous 2=longer delay fading in/out, 3=repeats
		pitchFactor = 1,		// multiplication factor for pitch
		gainFactor = 1,		// multiplication factor for volume
		wait = 3,			// initial wait before freeze kicks in
	 	startPos = 2,		// startposition for playback sample
	 	win, 				// parent window for GUI
	 	guiOn = 1,		// set to 0 if GUI should be hidden
	 	subOut = 8;		// output channel for sub

	^super.new.initMDFreeze(name, index, buf, version, lengthFactor, output, countDisp, envCurve, freezeType, pitchFactor, gainFactor, wait, startPos, win, guiOn, subOut);
	}

initMDFreeze
	{
	arg name, index, buf, version, lengthFactor, output, countDisp, envCurve, freezeType, pitchFactor, gainFactor, wait, startPos, win, guiOn, subOut;

	// ------- variables -------------------------------------------------------------

	var server = Server.default;
	var processFunc, countFunc, reverbSynth, processTask, countTask;
	var 	filterFade = 30, 	// release for LP filter in seconds
		filterAttack = 0.5, 	// attack for LP filter in seconds
		detailColor = Color.new255(231,185,106),
		volInit = 1; 		// initial volume
	var pFreezeType, pVersion, nGain, nPitch, nLength, bMute, lOutput;
	var sLevelBus = Bus.control(server,1), bMuteBus = Bus.control(server,1), nGainBus = Bus.control(server,1);
	var prefix = "";
	var top = 150;
	var subGain = 0.5;
	
	sLevelMainBus = Bus.control(server,1);
	
	// ------- SynthDefs -------------------------------------------------------------
		
	SynthDef(\bufferFreeze1_2ch ,{
		arg feedback = 0.5, hold = 0, gate, dieOut = 0.7, out1, out2, pitch, gainBus, length, start, volBus, muteBus, 
			openfilter = 0, volMainBus, sub;
		var in, localL, localR, sig, local, env;

		// Rand is different everytime the Synth starts
//		SendTrig.kr(Impulse.kr(4),1,Rand(0,10));
		
		in = BRF.ar(PlayBuf.ar(2, buf, BufRateScale.kr(buf)*pitch, startPos: server.sampleRate*start),110,0.5);
	
		local = LocalIn.ar(2);
		localL = (max(hold,0.1) * local[0]) + (in[0] * (1 - hold));
		localR = (max(hold,0.1) * local[1]) + (in[1] * (1 - hold));
		
		20.do{
			localL = AllpassC.ar(localL,0.05,Rand(0.001,0.05),Rand(1,3));
			localR = AllpassC.ar(localR,0.05,Rand(0.001,0.05),Rand(1,3));
			};
		LocalOut.ar([
//	  		localL*SinOsc.ar(LFNoise1.ar(1).range(0.03,0.1)).range(dieOut ,1),
//	  		localR*SinOsc.ar(LFNoise1.ar(1).range(0.03,0.1)).range(dieOut,1)
			localL,
			localR
				]*feedback);
	
		env = Select.ar(envCurve,
			EnvGen.ar(Env.linen(filterAttack,0,length*filterFade,1,\lin).range(500,15000), gate: openfilter, doneAction: 0),
			EnvGen.ar(Env.linen(filterAttack,0,length*filterFade,1,\exp).range(500,15000), gate: openfilter, doneAction: 0);
			);
		env = env + LFNoise1.ar(0.2).range(-300,1000);
//		SendTrig.kr(Impulse.kr(4),1,env);
	
		sig = [localL,localR] * EnvGen.kr(Env.adsr(0.01,0,1,5), gate, doneAction: 2);
		sig = HPF.ar(sig, 50);
		sig = LPF.ar(sig, env);
		sig = sig * In.kr(gainBus,1) * In.kr(volBus,1) * (1-In.kr(muteBus,1));
		sig = sig * In.kr(volMainBus,1);	
			
		Out.ar(out1, sig[0]);
		Out.ar(out2, sig[1]);
		Out.ar(sub, subGain*Mix.new(sig));
	}).send(server);

	SynthDef(\bufferFreeze1_4ch ,{
		arg feedback = 0.5, hold = 0, gate, dieOut = 0.7, out1, out2, out3, out4, pitch, gainBus, length, start, volBus, muteBus, 
			openfilter = 0, volMainBus, sub;
		var in, localL, localR, sig, local, env;

		// Rand is different everytime the Synth starts
//		SendTrig.kr(Impulse.kr(4),1,Rand(0,10));
		
		in = BRF.ar(PlayBuf.ar(2, buf, BufRateScale.kr(buf)*pitch, startPos: server.sampleRate*start),110,0.5);
	
		local = LocalIn.ar(2);
		localL = (max(hold,0.1) * local[0]) + (in[0] * (1 - hold));
		localR = (max(hold,0.1) * local[1]) + (in[1] * (1 - hold));
		
		20.do{
			localL = AllpassC.ar(localL,0.05,Rand(0.001,0.05),Rand(1,3));
			localR = AllpassC.ar(localR,0.05,Rand(0.001,0.05),Rand(1,3));
			};
		LocalOut.ar([
//	  		localL*SinOsc.ar(LFNoise1.ar(1).range(0.03,0.1)).range(dieOut ,1),
//	  		localR*SinOsc.ar(LFNoise1.ar(1).range(0.03,0.1)).range(dieOut,1)
			localL,
			localR
				]*feedback);
	
		env = Select.ar(envCurve,
			EnvGen.ar(Env.linen(filterAttack,0,length*filterFade,1,\lin).range(500,15000), gate: openfilter, doneAction: 0),
			EnvGen.ar(Env.linen(filterAttack,0,length*filterFade,1,\exp).range(500,15000), gate: openfilter, doneAction: 0);
			);
		env = env + LFNoise1.ar(0.2).range(-300,1000);
//		SendTrig.kr(Impulse.kr(4),1,env);
	
		sig = [localL,localR] * EnvGen.kr(Env.adsr(0.01,0,1,5), gate, doneAction: 2);
		sig = HPF.ar(sig, 50);
		sig = LPF.ar(sig, env);
		sig = sig * In.kr(gainBus,1) * In.kr(volBus,1) * (1-In.kr(muteBus,1));
		sig = sig * In.kr(volMainBus,1);	
			
		Out.ar(out1, sig[0]);
		Out.ar(out2, sig[1]);
		Out.ar(out3, sig[0]);
		Out.ar(out4, sig[1]);
		Out.ar(sub, subGain*Mix.new(sig));
	}).send(server);

	SynthDef(\bufferFreeze1_6ch ,{
		arg feedback = 0.5, hold = 0, gate, dieOut = 0.7, out1, out2, out3, out4, out5, out6, pitch, gainBus, length, start, volBus, muteBus,
			 openfilter = 0, volMainBus, sub;
		var in, localL, localR, sig, local, env;

		// Rand is different everytime the Synth starts
//		SendTrig.kr(Impulse.kr(4),1,Rand(0,10));
		
		in = BRF.ar(PlayBuf.ar(2, buf, BufRateScale.kr(buf)*pitch, startPos: server.sampleRate*start),110,0.5);
	
		local = LocalIn.ar(2);
		localL = (max(hold,0.1) * local[0]) + (in[0] * (1 - hold));
		localR = (max(hold,0.1) * local[1]) + (in[1] * (1 - hold));
		
		20.do{
			localL = AllpassC.ar(localL,0.05,Rand(0.001,0.05),Rand(1,3));
			localR = AllpassC.ar(localR,0.05,Rand(0.001,0.05),Rand(1,3));
			};
		LocalOut.ar([
//	  		localL*SinOsc.ar(LFNoise1.ar(1).range(0.03,0.1)).range(dieOut ,1),
//	  		localR*SinOsc.ar(LFNoise1.ar(1).range(0.03,0.1)).range(dieOut,1)
			localL,
			localR
				]*feedback);
	
		env = Select.ar(envCurve,
			EnvGen.ar(Env.linen(filterAttack,0,length*filterFade,1,\lin).range(500,15000), gate: openfilter, doneAction: 0),
			EnvGen.ar(Env.linen(filterAttack,0,length*filterFade,1,\exp).range(500,15000), gate: openfilter, doneAction: 0);
			);
		env = env + LFNoise1.ar(0.2).range(-300,1000);
//		SendTrig.kr(Impulse.kr(4),1,env);
	
		sig = [localL,localR] * EnvGen.kr(Env.adsr(0.01,0,1,5), gate, doneAction: 2);
		sig = HPF.ar(sig, 50);
		sig = LPF.ar(sig, env);
		sig = sig * In.kr(gainBus,1) * In.kr(volBus,1) * (1-In.kr(muteBus,1));
		sig = sig * In.kr(volMainBus,1);	
			
		Out.ar(out1, sig[0]);
		Out.ar(out2, sig[1]);
		Out.ar(out3, sig[0]);
		Out.ar(out4, sig[1]);
		Out.ar(out5, sig[0]);
		Out.ar(out6, sig[1]);
		Out.ar(sub, subGain*Mix.new(sig));
	}).send(server);

	SynthDef(\bufferFreeze1_8ch ,{
		arg feedback = 0.5, hold = 0, gate, dieOut = 0.7, out1, out2, out3, out4, out5, out6, out7, out8, pitch, gainBus, length, start, 
			volBus, muteBus, openfilter = 0, volMainBus, sub;
		var in, localL, localR, sig, local, env;

		// Rand is different everytime the Synth starts
//		SendTrig.kr(Impulse.kr(4),1,Rand(0,10));
		
		in = BRF.ar(PlayBuf.ar(2, buf, BufRateScale.kr(buf)*pitch, startPos: server.sampleRate*start),110,0.5);
	
		local = LocalIn.ar(2);
		localL = (max(hold,0.1) * local[0]) + (in[0] * (1 - hold));
		localR = (max(hold,0.1) * local[1]) + (in[1] * (1 - hold));
		
		20.do{
			localL = AllpassC.ar(localL,0.05,Rand(0.001,0.05),Rand(1,3));
			localR = AllpassC.ar(localR,0.05,Rand(0.001,0.05),Rand(1,3));
			};
		LocalOut.ar([
//	  		localL*SinOsc.ar(LFNoise1.ar(1).range(0.03,0.1)).range(dieOut ,1),
//	  		localR*SinOsc.ar(LFNoise1.ar(1).range(0.03,0.1)).range(dieOut,1)
			localL,
			localR
				]*feedback);
	
		env = Select.ar(envCurve,
			EnvGen.ar(Env.linen(filterAttack,0,length*filterFade,1,\lin).range(500,15000), gate: openfilter, doneAction: 0),
			EnvGen.ar(Env.linen(filterAttack,0,length*filterFade,1,\exp).range(500,15000), gate: openfilter, doneAction: 0);
			);
		env = env + LFNoise1.ar(0.2).range(-300,1000);
//		SendTrig.kr(Impulse.kr(4),1,env);
	
		sig = [localL,localR] * EnvGen.kr(Env.adsr(0.01,0,1,5), gate, doneAction: 2);
		sig = HPF.ar(sig, 50);
		sig = LPF.ar(sig, env);
		sig = sig * In.kr(gainBus,1) * In.kr(volBus,1) * (1-In.kr(muteBus,1));
		sig = sig * In.kr(volMainBus,1);	
			
		Out.ar(out1, sig[0]);
		Out.ar(out2, sig[1]);
		Out.ar(out3, sig[0]);
		Out.ar(out4, sig[1]);
		Out.ar(out5, sig[0]);
		Out.ar(out6, sig[1]);
		Out.ar(out7, sig[0]);
		Out.ar(out8, sig[1]);
		Out.ar(sub, subGain*Mix.new(sig));
	}).send(server);

	SynthDef(\bufferFreeze4_2ch ,{ // faster moving variation of bufferFreeze1
		arg feedback = 0.5, hold = 0, gate, dieOut = 0.7, out1, out2, pitch, gainBus, length, start, volBus, muteBus, volMainBus, sub;
		var in, localL, localR, sig, local, env, pulse, pulseTrig, pulseLen;
		
		in = BRF.ar(PlayBuf.ar(2, buf, BufRateScale.kr(buf)*pitch, startPos: server.sampleRate*start),110,0.5);
	
		local = LocalIn.ar(2);
		localL = (max(hold,0.1) * local[0]) + (in[0] * (1 - hold));
		localR = (max(hold,0.1) * local[1]) + (in[1] * (1 - hold));
		
		20.do{
			localL = AllpassC.ar(localL,0.05,Rand(0.001,0.05),Rand(1,3));
			localR = AllpassC.ar(localR,0.05,Rand(0.001,0.05),Rand(1,3));
			};
		LocalOut.ar([
			localL,
			localR
				]*feedback);
					
		env = LFNoise1.ar(0.2).range(300,1000);
	
		sig = [localL,localR] * EnvGen.kr(Env.adsr(0.01,0,1,5), gate, doneAction: 2);

		// sinusoid pulse
//		pulse = SinOsc.ar(SinOsc.ar(0.02).range(1.1,1.2)).range(0.2,1);

		// percussive pulse
		pulseLen = SinOsc.ar(0.02).range(1.1,1.2);
		pulseTrig = Impulse.ar(pulseLen);
		pulse = EnvGen.ar(Env.linen(0.02,0,pulseLen-0.1,1,\lin).range(0.2,1), gate: pulseTrig, doneAction: 0);

		sig = sig * pulse * SinOsc.ar(0.1).range(1,3);	
		sig = HPF.ar(sig, 50);
		sig = LPF.ar(sig, env);
		sig = sig * In.kr(gainBus,1) * In.kr(volBus,1) * (1-In.kr(muteBus,1));
		sig = sig * In.kr(volMainBus,1);	
			
		Out.ar(out1, sig[0]);
		Out.ar(out2, sig[1]);
		Out.ar(sub, subGain*Mix.new(sig));
	}).send(server);

	SynthDef(\bufferFreeze4_4ch ,{ // faster moving variation of bufferFreeze1
		arg feedback = 0.5, hold = 0, gate, dieOut = 0.7, out1, out2, out3, out4, pitch, gainBus, length, start, volBus, muteBus, 
			volMainBus, sub;
		var in, localL, localR, sig, local, env, pulse, pulseTrig, pulseLen;
		
		in = BRF.ar(PlayBuf.ar(2, buf, BufRateScale.kr(buf)*pitch, startPos: server.sampleRate*start),110,0.5);
	
		local = LocalIn.ar(2);
		localL = (max(hold,0.1) * local[0]) + (in[0] * (1 - hold));
		localR = (max(hold,0.1) * local[1]) + (in[1] * (1 - hold));
		
		20.do{
			localL = AllpassC.ar(localL,0.05,Rand(0.001,0.05),Rand(1,3));
			localR = AllpassC.ar(localR,0.05,Rand(0.001,0.05),Rand(1,3));
			};
		LocalOut.ar([
			localL,
			localR
				]*feedback);
					
		env = LFNoise1.ar(0.2).range(300,1000);
	
		sig = [localL,localR] * EnvGen.kr(Env.adsr(0.01,0,1,5), gate, doneAction: 2);

		// sinusoid pulse
//		pulse = SinOsc.ar(SinOsc.ar(0.02).range(1.1,1.2)).range(0.2,1);

		// percussive pulse
		pulseLen = SinOsc.ar(0.02).range(1.1,1.2);
		pulseTrig = Impulse.ar(pulseLen);
		pulse = EnvGen.ar(Env.linen(0.02,0,pulseLen-0.1,1,\lin).range(0.2,1), gate: pulseTrig, doneAction: 0);

		sig = sig * pulse * SinOsc.ar(0.1).range(1,3);	
		sig = HPF.ar(sig, 50);
		sig = LPF.ar(sig, env);
		sig = sig * In.kr(gainBus,1) * In.kr(volBus,1) * (1-In.kr(muteBus,1));
		sig = sig * In.kr(volMainBus,1);	
			
		Out.ar(out1, sig[0]);
		Out.ar(out2, sig[1]);
		Out.ar(out3, sig[0]);
		Out.ar(out4, sig[1]);
		Out.ar(sub, subGain*Mix.new(sig));
	}).send(server);

	SynthDef(\bufferFreeze4_6ch ,{ // faster moving variation of bufferFreeze1
		arg feedback = 0.5, hold = 0, gate, dieOut = 0.7, out1, out2, out3, out4, out5, out6, pitch, gainBus, length, start, volBus, 
			muteBus, volMainBus, sub;
		var in, localL, localR, sig, local, env, pulse, pulseTrig, pulseLen;
		
		in = BRF.ar(PlayBuf.ar(2, buf, BufRateScale.kr(buf)*pitch, startPos: server.sampleRate*start),110,0.5);
	
		local = LocalIn.ar(2);
		localL = (max(hold,0.1) * local[0]) + (in[0] * (1 - hold));
		localR = (max(hold,0.1) * local[1]) + (in[1] * (1 - hold));
		
		20.do{
			localL = AllpassC.ar(localL,0.05,Rand(0.001,0.05),Rand(1,3));
			localR = AllpassC.ar(localR,0.05,Rand(0.001,0.05),Rand(1,3));
			};
		LocalOut.ar([
			localL,
			localR
				]*feedback);
					
		env = LFNoise1.ar(0.2).range(300,1000);
	
		sig = [localL,localR] * EnvGen.kr(Env.adsr(0.01,0,1,5), gate, doneAction: 2);

		// sinusoid pulse
//		pulse = SinOsc.ar(SinOsc.ar(0.02).range(1.1,1.2)).range(0.2,1);

		// percussive pulse
		pulseLen = SinOsc.ar(0.02).range(1.1,1.2);
		pulseTrig = Impulse.ar(pulseLen);
		pulse = EnvGen.ar(Env.linen(0.02,0,pulseLen-0.1,1,\lin).range(0.2,1), gate: pulseTrig, doneAction: 0);

		sig = sig * pulse * SinOsc.ar(0.1).range(1,3);	
		sig = HPF.ar(sig, 50);
		sig = LPF.ar(sig, env);
		sig = sig * In.kr(gainBus,1) * In.kr(volBus,1) * (1-In.kr(muteBus,1));
		sig = sig * In.kr(volMainBus,1);	
			
		Out.ar(out1, sig[0]);
		Out.ar(out2, sig[1]);
		Out.ar(out3, sig[0]);
		Out.ar(out4, sig[1]);
		Out.ar(out5, sig[0]);
		Out.ar(out6, sig[1]);
		Out.ar(sub, subGain*Mix.new(sig));
	}).send(server);

	SynthDef(\bufferFreeze4_8ch ,{ // faster moving variation of bufferFreeze1
		arg feedback = 0.5, hold = 0, gate, dieOut = 0.7, out1, out2, out3, out4, out5, out6, out7, out8, pitch, gainBus, length, start, 
			volBus, muteBus, volMainBus, sub;
		var in, localL, localR, sig, local, env, pulse, pulseTrig, pulseLen;
		
		in = BRF.ar(PlayBuf.ar(2, buf, BufRateScale.kr(buf)*pitch, startPos: server.sampleRate*start),110,0.5);
	
		local = LocalIn.ar(2);
		localL = (max(hold,0.1) * local[0]) + (in[0] * (1 - hold));
		localR = (max(hold,0.1) * local[1]) + (in[1] * (1 - hold));
		
		20.do{
			localL = AllpassC.ar(localL,0.05,Rand(0.001,0.05),Rand(1,3));
			localR = AllpassC.ar(localR,0.05,Rand(0.001,0.05),Rand(1,3));
			};
		LocalOut.ar([
			localL,
			localR
				]*feedback);
					
		env = LFNoise1.ar(0.2).range(300,1000);
	
		sig = [localL,localR] * EnvGen.kr(Env.adsr(0.01,0,1,5), gate, doneAction: 2);

		// sinusoid pulse
//		pulse = SinOsc.ar(SinOsc.ar(0.02).range(1.1,1.2)).range(0.2,1);

		// percussive pulse
		pulseLen = SinOsc.ar(0.02).range(1.1,1.2);
		pulseTrig = Impulse.ar(pulseLen);
		pulse = EnvGen.ar(Env.linen(0.02,0,pulseLen-0.1,1,\lin).range(0.2,1), gate: pulseTrig, doneAction: 0);

		sig = sig * pulse * SinOsc.ar(0.1).range(1,3);	
		sig = HPF.ar(sig, 50);
		sig = LPF.ar(sig, env);
		sig = sig * In.kr(gainBus,1) * In.kr(volBus,1) * (1-In.kr(muteBus,1));
		sig = sig * In.kr(volMainBus,1);	
			
		Out.ar(out1, sig[0]);
		Out.ar(out2, sig[1]);
		Out.ar(out3, sig[0]);
		Out.ar(out4, sig[1]);
		Out.ar(out5, sig[0]);
		Out.ar(out6, sig[1]);
		Out.ar(out7, sig[0]);
		Out.ar(out8, sig[1]);
		Out.ar(sub, subGain*Mix.new(sig));
	}).send(server);

	SynthDef(\bufferFreeze2_2ch ,{
		arg feedback = 0.5, hold = 0, gate, dieOut = 0.7, out1, out2, pitch, gainBus, length, start, volBus, muteBus, openfilter = 0, 
			volMainBus, sub;
		var in, localL, localR, sig, local, env;
		
		in = BRF.ar(PlayBuf.ar(2, buf, BufRateScale.kr(buf)*pitch, startPos: server.sampleRate*start),110,0.5);
	
		local = LocalIn.ar(2);
		localL = (max(hold,0.1) * local[0]) + (in[0] * (1 - hold));
		localR = (max(hold,0.1) * local[1]) + (in[1] * (1 - hold));
	
	  	20.do{
	  		localL = AllpassC.ar(localL,1,Rand(0.1,1),Rand(3,5));
	  		localR = AllpassC.ar(localR,1,Rand(0.1,1),Rand(3,5));
	  		};
	  	LocalOut.ar([
	  		localL*SinOsc.ar(LFNoise1.ar(1).range(0.03,0.1)).range(dieOut ,1.1),
	  		localR*SinOsc.ar(LFNoise1.ar(1).range(0.03,0.1)).range(dieOut,1.2)
	  			]*feedback);
		
		env = Select.ar(envCurve,
			EnvGen.ar(Env.linen(filterAttack,0,length*filterFade,1,\lin).range(500,15000), gate: openfilter, doneAction: 0),
			EnvGen.ar(Env.linen(filterAttack,0,length*filterFade,1,\exp).range(500,15000), gate: openfilter, doneAction: 0);
			);
		env = env + LFNoise1.ar(0.2).range(-300,1000);
	
		sig = [localL,localR] * EnvGen.kr(Env.adsr(0.01,0,1,5), gate, doneAction: 2);
		sig = HPF.ar(sig, 50);
		sig = LPF.ar(sig, env);			
		sig = sig * In.kr(gainBus,1) * In.kr(volBus,1) * (1-In.kr(muteBus,1));
		sig = sig * In.kr(volMainBus,1);	

//		SendTrig.kr(Impulse.kr(4),1,In.kr(volMainBus,1));
			
		Out.ar(out1, sig[0]);
		Out.ar(out2, sig[1]);
		Out.ar(sub, subGain*Mix.new(sig));
	}).send(server);

	SynthDef(\bufferFreeze2_4ch ,{
		arg feedback = 0.5, hold = 0, gate, dieOut = 0.7, out1, out2, out3, out4, pitch, gainBus, length, start, volBus, muteBus, 
			openfilter = 0, volMainBus, sub;
		var in, localL, localR, sig, local, env;
		
		in = BRF.ar(PlayBuf.ar(2, buf, BufRateScale.kr(buf)*pitch, startPos: server.sampleRate*start),110,0.5);
	
		local = LocalIn.ar(2);
		localL = (max(hold,0.1) * local[0]) + (in[0] * (1 - hold));
		localR = (max(hold,0.1) * local[1]) + (in[1] * (1 - hold));
	
	  	20.do{
	  		localL = AllpassC.ar(localL,1,Rand(0.1,1),Rand(3,5));
	  		localR = AllpassC.ar(localR,1,Rand(0.1,1),Rand(3,5));
	  		};
	  	LocalOut.ar([
	  		localL*SinOsc.ar(LFNoise1.ar(1).range(0.03,0.1)).range(dieOut ,1.1),
	  		localR*SinOsc.ar(LFNoise1.ar(1).range(0.03,0.1)).range(dieOut,1.2)
	  			]*feedback);
		
		env = Select.ar(envCurve,
			EnvGen.ar(Env.linen(filterAttack,0,length*filterFade,1,\lin).range(500,15000), gate: openfilter, doneAction: 0),
			EnvGen.ar(Env.linen(filterAttack,0,length*filterFade,1,\exp).range(500,15000), gate: openfilter, doneAction: 0);
			);
		env = env + LFNoise1.ar(0.2).range(-300,1000);
	
		sig = [localL,localR] * EnvGen.kr(Env.adsr(0.01,0,1,5), gate, doneAction: 2);
		sig = HPF.ar(sig, 50);
		sig = LPF.ar(sig, env);			
		sig = sig * In.kr(gainBus,1) * In.kr(volBus,1) * (1-In.kr(muteBus,1));
		sig = sig * In.kr(volMainBus,1);	
			
		Out.ar(out1, sig[0]);
		Out.ar(out2, sig[1]);
		Out.ar(out3, sig[0]);
		Out.ar(out4, sig[1]);
		Out.ar(sub, subGain*Mix.new(sig));
	}).send(server);

	SynthDef(\bufferFreeze2_6ch ,{
		arg feedback = 0.5, hold = 0, gate, dieOut = 0.7, out1, out2, out3, out4, out5, out6, pitch, gainBus, length, start, volBus, 
			muteBus, openfilter = 0, volMainBus, sub;
		var in, localL, localR, sig, local, env;
		
		in = BRF.ar(PlayBuf.ar(2, buf, BufRateScale.kr(buf)*pitch, startPos: server.sampleRate*start),110,0.5);
	
		local = LocalIn.ar(2);
		localL = (max(hold,0.1) * local[0]) + (in[0] * (1 - hold));
		localR = (max(hold,0.1) * local[1]) + (in[1] * (1 - hold));
	
	  	20.do{
	  		localL = AllpassC.ar(localL,1,Rand(0.1,1),Rand(3,5));
	  		localR = AllpassC.ar(localR,1,Rand(0.1,1),Rand(3,5));
	  		};
	  	LocalOut.ar([
	  		localL*SinOsc.ar(LFNoise1.ar(1).range(0.03,0.1)).range(dieOut ,1.1),
	  		localR*SinOsc.ar(LFNoise1.ar(1).range(0.03,0.1)).range(dieOut,1.2)
	  			]*feedback);
		
		env = Select.ar(envCurve,
			EnvGen.ar(Env.linen(filterAttack,0,length*filterFade,1,\lin).range(500,15000), gate: openfilter, doneAction: 0),
			EnvGen.ar(Env.linen(filterAttack,0,length*filterFade,1,\exp).range(500,15000), gate: openfilter, doneAction: 0);
			);
		env = env + LFNoise1.ar(0.2).range(-300,1000);
	
		sig = [localL,localR] * EnvGen.kr(Env.adsr(0.01,0,1,5), gate, doneAction: 2);
		sig = HPF.ar(sig, 50);
		sig = LPF.ar(sig, env);			
		sig = sig * In.kr(gainBus,1) * In.kr(volBus,1) * (1-In.kr(muteBus,1));
		sig = sig * In.kr(volMainBus,1);	
			
		Out.ar(out1, sig[0]);
		Out.ar(out2, sig[1]);
		Out.ar(out3, sig[0]);
		Out.ar(out4, sig[1]);
		Out.ar(out5, sig[0]);
		Out.ar(out6, sig[1]);
		Out.ar(sub, subGain*Mix.new(sig));
	}).send(server);

	SynthDef(\bufferFreeze2_8ch ,{
		arg feedback = 0.5, hold = 0, gate, dieOut = 0.7, out1, out2, out3, out4, out5, out6, out7, out8, pitch, gainBus, length, start, 
			volBus, muteBus, openfilter = 0, volMainBus, sub;
		var in, localL, localR, sig, local, env;
		
		in = BRF.ar(PlayBuf.ar(2, buf, BufRateScale.kr(buf)*pitch, startPos: server.sampleRate*start),110,0.5);
	
		local = LocalIn.ar(2);
		localL = (max(hold,0.1) * local[0]) + (in[0] * (1 - hold));
		localR = (max(hold,0.1) * local[1]) + (in[1] * (1 - hold));
	
	  	20.do{
	  		localL = AllpassC.ar(localL,1,Rand(0.1,1),Rand(3,5));
	  		localR = AllpassC.ar(localR,1,Rand(0.1,1),Rand(3,5));
	  		};
	  	LocalOut.ar([
	  		localL*SinOsc.ar(LFNoise1.ar(1).range(0.03,0.1)).range(dieOut ,1.1),
	  		localR*SinOsc.ar(LFNoise1.ar(1).range(0.03,0.1)).range(dieOut,1.2)
	  			]*feedback);
		
		env = Select.ar(envCurve,
			EnvGen.ar(Env.linen(filterAttack,0,length*filterFade,1,\lin).range(500,15000), gate: openfilter, doneAction: 0),
			EnvGen.ar(Env.linen(filterAttack,0,length*filterFade,1,\exp).range(500,15000), gate: openfilter, doneAction: 0);
			);
		env = env + LFNoise1.ar(0.2).range(-300,1000);
	
		sig = [localL,localR] * EnvGen.kr(Env.adsr(0.01,0,1,5), gate, doneAction: 2);
		sig = HPF.ar(sig, 50);
		sig = LPF.ar(sig, env);			
		sig = sig * In.kr(gainBus,1) * In.kr(volBus,1) * (1-In.kr(muteBus,1));
		sig = sig * In.kr(volMainBus,1);	
			
		Out.ar(out1, sig[0]);
		Out.ar(out2, sig[1]);
		Out.ar(out3, sig[0]);
		Out.ar(out4, sig[1]);
		Out.ar(out5, sig[0]);
		Out.ar(out6, sig[1]);
		Out.ar(out7, sig[0]);
		Out.ar(out8, sig[1]);
		Out.ar(sub, subGain*Mix.new(sig));
	}).send(server);

	SynthDef(\bufferFreeze3_2ch ,{
		arg feedback = 0.5, hold = 0, gate, dieOut = 0.7, out1, out2, pitch, gainBus, length, start, volBus, muteBus, openfilter = 0, 
			volMainBus, sub;
		var in, localL, localR, sig, local, env;
		
		in = BRF.ar(PlayBuf.ar(2, buf, BufRateScale.kr(buf)*pitch, startPos: server.sampleRate*start),110,0.5);
	
		local = LocalIn.ar(2);
		localL = (max(hold,0.1) * local[0]) + (in[0] * (1 - hold));
		localR = (max(hold,0.1) * local[1]) + (in[1] * (1 - hold));
		
	  	1.do{
	  		localL = AllpassC.ar(localL,5,Rand(0.15,0.8),3); // max delay was 5
	  		localR = AllpassC.ar(localR,4,Rand(0.1,0.7),3); // max delay was 4
	  		};
		LocalOut.ar([
			localL,
			localR
				]*feedback);
	
		env = Select.ar(envCurve,
			EnvGen.ar(Env.linen(filterAttack,0,length*filterFade,1,\lin).range(400,1000), gate: openfilter, doneAction: 0),
			EnvGen.ar(Env.linen(filterAttack,0,length*filterFade,1,\exp).range(400,1000), gate: openfilter, doneAction: 0);
			);
		env = env + LFNoise1.ar(0.2).range(-300,500);
	
		sig = [localL,localR] * EnvGen.kr(Env.adsr(0.01,0,1,5), gate, doneAction: 2);
		sig = HPF.ar(sig, 50);
		sig = LPF.ar(sig, env);
		sig = sig * In.kr(gainBus,1) * In.kr(volBus,1) * (1-In.kr(muteBus,1));
		sig = sig * In.kr(volMainBus,1);	
			
		Out.ar(out1, sig[0]);
		Out.ar(out2, sig[1]);
		Out.ar(sub, subGain*Mix.new(sig));
	}).send(server);

	SynthDef(\bufferFreeze3_4ch ,{
		arg feedback = 0.5, hold = 0, gate, dieOut = 0.7, out1, out2, out3, out4, pitch, gainBus, length, start, volBus, muteBus, 
			openfilter = 0, volMainBus, sub;
		var in, localL, localR, sig, local, env;
		
		in = BRF.ar(PlayBuf.ar(2, buf, BufRateScale.kr(buf)*pitch, startPos: server.sampleRate*start),110,0.5);
	
		local = LocalIn.ar(2);
		localL = (max(hold,0.1) * local[0]) + (in[0] * (1 - hold));
		localR = (max(hold,0.1) * local[1]) + (in[1] * (1 - hold));
		
	  	1.do{
	  		localL = AllpassC.ar(localL,5,Rand(0.15,0.8),3); // max delay was 5
	  		localR = AllpassC.ar(localR,4,Rand(0.1,0.7),3); // max delay was 4
	  		};
		LocalOut.ar([
			localL,
			localR
				]*feedback);
	
		env = Select.ar(envCurve,
			EnvGen.ar(Env.linen(filterAttack,0,length*filterFade,1,\lin).range(400,1000), gate: openfilter, doneAction: 0),
			EnvGen.ar(Env.linen(filterAttack,0,length*filterFade,1,\exp).range(400,1000), gate: openfilter, doneAction: 0);
			);
		env = env + LFNoise1.ar(0.2).range(-300,500);
	
		sig = [localL,localR] * EnvGen.kr(Env.adsr(0.01,0,1,5), gate, doneAction: 2);
		sig = HPF.ar(sig, 50);
		sig = LPF.ar(sig, env);
		sig = sig * In.kr(gainBus,1) * In.kr(volBus,1) * (1-In.kr(muteBus,1));
		sig = sig * In.kr(volMainBus,1);	
			
		Out.ar(out1, sig[0]);
		Out.ar(out2, sig[1]);
		Out.ar(out3, sig[0]);
		Out.ar(out4, sig[1]);
		Out.ar(sub, subGain*Mix.new(sig));
	}).send(server);

	SynthDef(\bufferFreeze3_6ch ,{
		arg feedback = 0.5, hold = 0, gate, dieOut = 0.7, out1, out2, out3, out4, out5, out6, pitch, gainBus, length, start, volBus, muteBus,
			 openfilter = 0, volMainBus, sub;
		var in, localL, localR, sig, local, env;
		
		in = BRF.ar(PlayBuf.ar(2, buf, BufRateScale.kr(buf)*pitch, startPos: server.sampleRate*start),110,0.5);
	
		local = LocalIn.ar(2);
		localL = (max(hold,0.1) * local[0]) + (in[0] * (1 - hold));
		localR = (max(hold,0.1) * local[1]) + (in[1] * (1 - hold));
		
	  	1.do{
	  		localL = AllpassC.ar(localL,5,Rand(0.15,0.8),3); // max delay was 5
	  		localR = AllpassC.ar(localR,4,Rand(0.1,0.7),3); // max delay was 4
	  		};
		LocalOut.ar([
			localL,
			localR
				]*feedback);
	
		env = Select.ar(envCurve,
			EnvGen.ar(Env.linen(filterAttack,0,length*filterFade,1,\lin).range(400,1000), gate: openfilter, doneAction: 0),
			EnvGen.ar(Env.linen(filterAttack,0,length*filterFade,1,\exp).range(400,1000), gate: openfilter, doneAction: 0);
			);
		env = env + LFNoise1.ar(0.2).range(-300,500);
	
		sig = [localL,localR] * EnvGen.kr(Env.adsr(0.01,0,1,5), gate, doneAction: 2);
		sig = HPF.ar(sig, 50);
		sig = LPF.ar(sig, env);	
		sig = sig * In.kr(gainBus,1) * In.kr(volBus,1) * (1-In.kr(muteBus,1));
		sig = sig * In.kr(volMainBus,1);	
			
		Out.ar(out1, sig[0]);
		Out.ar(out2, sig[1]);
		Out.ar(out3, sig[0]);
		Out.ar(out4, sig[1]);
		Out.ar(out5, sig[0]);
		Out.ar(out6, sig[1]);
		Out.ar(sub, subGain*Mix.new(sig));
	}).send(server);

	SynthDef(\bufferFreeze3_8ch ,{
		arg feedback = 0.5, hold = 0, gate, dieOut = 0.7, out1, out2, out3, out4, out5, out6, out7, out8, pitch, gainBus, length, start, 
			volBus, muteBus, openfilter = 0, volMainBus, sub;
		var in, localL, localR, sig, local, env;
		
		in = BRF.ar(PlayBuf.ar(2, buf, BufRateScale.kr(buf)*pitch, startPos: server.sampleRate*start),110,0.5);
	
		local = LocalIn.ar(2);
		localL = (max(hold,0.1) * local[0]) + (in[0] * (1 - hold));
		localR = (max(hold,0.1) * local[1]) + (in[1] * (1 - hold));
		
	  	1.do{
	  		localL = AllpassC.ar(localL,5,Rand(0.15,0.8),3); // max delay was 5
	  		localR = AllpassC.ar(localR,4,Rand(0.1,0.7),3); // max delay was 4
	  		};
		LocalOut.ar([
			localL,
			localR
				]*feedback);
	
		env = Select.ar(envCurve,
			EnvGen.ar(Env.linen(filterAttack,0,length*filterFade,1,\lin).range(400,1000), gate: openfilter, doneAction: 0),
			EnvGen.ar(Env.linen(filterAttack,0,length*filterFade,1,\exp).range(400,1000), gate: openfilter, doneAction: 0);
			);
		env = env + LFNoise1.ar(0.2).range(-300,500);
	
		sig = [localL,localR] * EnvGen.kr(Env.adsr(0.01,0,1,5), gate, doneAction: 2);
		sig = HPF.ar(sig, 50);
		sig = LPF.ar(sig, env);		
		sig = sig * In.kr(gainBus,1) * In.kr(volBus,1) * (1-In.kr(muteBus,1));
		sig = sig * In.kr(volMainBus,1);	
			
		Out.ar(out1, sig[0]);
		Out.ar(out2, sig[1]);
		Out.ar(out3, sig[0]);
		Out.ar(out4, sig[1]);
		Out.ar(out5, sig[0]);
		Out.ar(out6, sig[1]);
		Out.ar(out7, sig[0]);
		Out.ar(out8, sig[1]);
		Out.ar(sub, subGain*Mix.new(sig));
	}).send(server);
		
	// ------- functions -------------------------------------------------------------
	
	processFunc = {
		reverbSynth.set(\hold,1);
		reverbSynth.set(\openfilter,1);
		reverbSynth.set(\feedback, 1);
		2.do({|i|
			(lengthFactor * rrand(40,50)).wait;
			reverbSynth.set(\openfilter,0); 0.1.wait; reverbSynth.set(\openfilter,1);
			(prefix ++ name + ">>>>> OPEN (a" ++ i ++ ") >>>>>").postln;
			reverbSynth.set(\hold,0);
			reverbSynth.set(\feedback, 0);	
			rrand(0.1,0.5).wait;	
			(prefix ++ name + ">>>>> CLOSE (a" ++ i ++ ") >>>>>").postln;
			reverbSynth.set(\hold,1);
			reverbSynth.set(\feedback, 1);
			});
		2.do({|i|
			(lengthFactor * rrand(20,30)).wait;
			reverbSynth.set(\openfilter,0); 0.1.wait; reverbSynth.set(\openfilter,1);
			(prefix ++ name + ">>>>> OPEN (b" ++ i ++ ") >>>>>").postln;
			reverbSynth.set(\hold,0);
			reverbSynth.set(\feedback, 0);	
			rrand(0.3,0.7).wait;	
			(prefix ++ name + ">>>>> CLOSE (b" ++ i ++ ") >>>>>").postln;
			reverbSynth.set(\hold,1);
			reverbSynth.set(\feedback, 1);
			});
		3.do({|i|
			(lengthFactor * rrand(10,20)).wait;
			reverbSynth.set(\openfilter,0); 0.1.wait; reverbSynth.set(\openfilter,1);
			(prefix ++ name + ">>>>> OPEN (c" ++ i ++ ") >>>>>").postln;
			reverbSynth.set(\hold,0);
			reverbSynth.set(\feedback, 0);	
			rrand(0.4,0.9).wait;	
			(prefix ++ name + ">>>>> CLOSE (c" ++ i ++ ") >>>>>").postln;
			reverbSynth.set(\hold,1);
			reverbSynth.set(\feedback, 1);
			reverbSynth.set(\dieOut, 0.5);
			});
		1.do({|i|
			(lengthFactor * rrand(80,90)).wait;
			reverbSynth.set(\openfilter,0); 0.1.wait; reverbSynth.set(\openfilter,1);
			(prefix ++ name + ">>>>> OPEN (C" ++ i ++ ") >>>>>").postln;
			reverbSynth.set(\hold,0);
			reverbSynth.set(\feedback, 0);	
			rrand(0.1,0.5).wait;	
			(prefix ++ name + ">>>>> CLOSE (C" ++ i ++ ") >>>>>").postln;
			reverbSynth.set(\hold,1);
			reverbSynth.set(\feedback, 1);
			reverbSynth.set(\dieOut, 0.7);
			});
		if(version == 1, {
			3.do({|i|
				(lengthFactor * rrand(30,40)).wait;
				reverbSynth.set(\openfilter,0); 0.1.wait; reverbSynth.set(\openfilter,1);
				(prefix ++ name + ">>>>> OPEN (d" ++ i ++ ") >>>>>").postln;
				reverbSynth.set(\hold,0);
				reverbSynth.set(\feedback, 0);	
				rrand(0.1,0.5).wait;	
				(prefix ++ name + ">>>>> CLOSE (d" ++ i ++ ") >>>>>").postln;
				reverbSynth.set(\hold,1);
				reverbSynth.set(\feedback, 1);
				reverbSynth.set(\dieOut, 0.7);
				});
			6.do({|i|
				(lengthFactor * rrand(10,20)).wait;
				reverbSynth.set(\openfilter,0); 0.1.wait; reverbSynth.set(\openfilter,1);
				(prefix ++ name + ">>>>> OPEN (e" ++ i ++ ") >>>>>").postln;
				reverbSynth.set(\hold,0);
				reverbSynth.set(\feedback, 0);	
				rrand(0.4,0.9).wait;	
				(prefix ++ name + ">>>>> CLOSE (e" ++ i ++ ") >>>>>").postln;
				reverbSynth.set(\hold,1);
				reverbSynth.set(\feedback, 1);
				reverbSynth.set(\dieOut, 0.5);
				});
			1.do({|i|
				(lengthFactor * rrand(50,60)).wait;
				reverbSynth.set(\openfilter,0); 0.1.wait; reverbSynth.set(\openfilter,1);
				(prefix ++ name + ">>>>> OPEN (f" ++ i ++ ") >>>>>").postln;
				reverbSynth.set(\hold,0);
				reverbSynth.set(\feedback, 0);	
				rrand(0.1,0.5).wait;	
				(prefix ++ name + ">>>>> CLOSE (f" ++ i ++ ") >>>>>").postln;
				reverbSynth.set(\hold,1);
				reverbSynth.set(\feedback, 1);
				reverbSynth.set(\dieOut, 0.7);
				});
			});
		};
	
	countFunc = {
		case 
			{output.size == 2}
			{
			reverbSynth = Synth(\bufferFreeze++freezeType++"_2ch", [
				\feedback,0.5,\hold,1,\gate,1,
				\out1, output[0].asInt,\out2, output[1].asInt,
				\pitch,pitchFactor, \length,lengthFactor,\start,startPos,
				\volBus, sLevelBus.index,\muteBus,bMuteBus.index, \gainBus,nGainBus.index, \openfilter, 1,
				\volMainBus, sLevelMainBus.index, \sub, subOut
				]);
			}
			{output.size == 4}
			{
			reverbSynth = Synth(\bufferFreeze++freezeType++"_4ch", [
				\feedback,0.5,\hold,1,\gate,1,
				\out1, output[0].asInt,\out2, output[1].asInt,\out3, output[2].asInt,\out4, output[3].asInt,
				\pitch,pitchFactor, \length,lengthFactor,\start,startPos,
				\volBus, sLevelBus.index,\muteBus,bMuteBus.index, \gainBus,nGainBus.index, \openfilter, 1,
				\volMainBus, sLevelMainBus.index, \sub, subOut
				]);
			}
			{output.size == 6}
			{
			reverbSynth = Synth(\bufferFreeze++freezeType++"_6ch", [
				\feedback,0.5,\hold,1,\gate,1,
				\out1, output[0].asInt,\out2, output[1].asInt,\out3, output[2].asInt,\out4, output[3].asInt,
					\out5, output[4].asInt,\out6, output[5].asInt,
				\pitch,pitchFactor, \length,lengthFactor,\start,startPos,
				\volBus, sLevelBus.index,\muteBus,bMuteBus.index, \gainBus,nGainBus.index, \openfilter, 1,
				\volMainBus, sLevelMainBus.index, \sub, subOut
				]);
			}
			{output.size == 8}
			{
			reverbSynth = Synth(\bufferFreeze++freezeType++"_8ch", [
				\feedback,0.5,\hold,1,\gate,1,
				\out1, output[0].asInt,\out2, output[1].asInt,\out3, output[2].asInt,\out4, output[3].asInt,
					\out5, output[4].asInt,\out6, output[5].asInt,\out7, output[6].asInt,\out8, output[7].asInt,
				\pitch,pitchFactor, \length,lengthFactor,\start,startPos,
				\volBus, sLevelBus.index,\muteBus,bMuteBus.index, \gainBus,nGainBus.index, \openfilter, 1,
				\volMainBus, sLevelMainBus.index, \sub, subOut
				]);
			}
			;

		{reverbSynth.set(\hold,0)}.defer(0.1);
		{processTask = Task(processFunc).play}.defer(wait + 1.0.rand);
		(if(version==1,{600},{330})*lengthFactor).do({
			arg i;
		 	if(countDisp==1, {(prefix ++ name + (i.asTimeString)).postln});
		 	1.wait;
			});
			
		// stop process
//		reverbSynth.set(\gate,0);
//		processTask.stop;
		{if(bStart.value == 1, {bStart.valueAction_(0); bStart.enabled = true;})}.defer(); 
		
		};

	// ------- GUI -------------------------------------------------------------

	sLevel = EZSlider(win, Rect(30, top+((index-1)*80), 340, 18), "Track"+name+"        ", 
		ControlSpec(0.ampdb, 1.ampdb, \db, units: "dB", step: 0.01), 
		numberWidth: 40, layout: \horz, unitWidth: 25, initVal: volInit, labelWidth: 75,
		action: {
			sLevelBus.set(sLevel.value.dbamp);
			});
	sLevel.setColors(detailColor,Color.black, Color.white,detailColor, Color.black, Color.yellow, Color.yellow, detailColor);
	sLevel.font_(Font("Helvetica",12));
	sLevel.round_(0.01);

	nGain = EZNumber(win, Rect(380, top+((index-1)*80), 110, 18), "Gain  ", 
		ControlSpec(0.ampdb,10.ampdb, \db, units: "dB", step:0.01), 
		action: {nGainBus.set(nGain.value.dbamp); ("Track"+name+"gain set to"+nGain.value.dbamp).postln;},
		initVal: gainFactor.ampdb, labelWidth: 40, unitWidth:25);
	nGain.font_(Font("Helvetica",12));
	nGain.setColors(detailColor,Color.black, Color.white, Color.black);

	lOutput = SCStaticText(win, Rect(500, top+((index-1)*80), 58, 18));
	lOutput.string = " OutChan";
	lOutput.background = detailColor;
	
	tOutput = SCTextField(win, Rect(560, top+((index-1)*80), 80, 18));
	tOutput.font_(Font("Helvetica",12));
	tOutput.string = subStr(output.asString,2,output.asString.size-2).replace(" ",""); 
	tOutput.action = ({
		output = tOutput.value.split($,);
//		(output.size)%2.postln;
		case 
			{(output.size)%2 == 0}
			{
			("Track"+name+"output set to"+output ).postln;
			}
			{(output.size)%2 == 1}
			{
			output = [0,1];
			tOutput.value = "0,1";
			("Track"+name+"error: the output array should have an even number of entries, comma separated, max 8").postln;
			}
			;
		});

	bStart = SCButton(win, Rect(650, top+((index-1)*80), 40, 18));
	bStart.canFocus = false;
	bStart.states = [["Start",Color.black,Color.white],["Stop",Color.white,Color.black]];
	bStart.action = {
		if(bStart.value == 1, {
			("Start track" + name).postln;
			{countTask = Task(countFunc).play}.defer(0.5);
			
			tOutput.enabled = false;
			pFreezeType.enabled = false;
			pVersion.enabled = false;
			nLength.enabled = false;
			nLength.numberView.enabled = false;
			nPitch.enabled = false;
			nPitch.numberView.enabled = false;
			},
			{
			("Stop track" + name).postln;
			reverbSynth.set(\gate,0);
			countTask.stop; 
			{processTask.stop}.defer(wait+1); 
		
			tOutput.enabled = true;
			pFreezeType.enabled = true;
			pVersion.enabled = true;
			nLength.enabled = true;
			nLength.numberView.enabled = true;
			nPitch.enabled = true;
			nPitch.numberView.enabled = true;
			});
		}; 
	
	pFreezeType = EZPopUpMenu(win, Rect(30, top+30+((index-1)*80), 110, 18), "Type  ", 
		[
		\freeze1 -> {arg a; freezeType = 1; ("Track"+name+"freezeType set to"+freezeType).postln;},
		\freeze2 -> {arg a; freezeType = 2; ("Track"+name+"freezeType set to"+freezeType).postln;},
		\freeze3 -> {arg a; freezeType = 3; ("Track"+name+"freezeType set to"+freezeType).postln;},
		\freeze4 -> {arg a; freezeType = 4; ("Track"+name+"freezeType set to"+freezeType).postln;}
		],
		initVal: freezeType - 1, labelWidth: 40);
	pFreezeType.font_(Font("Helvetica",12));
	pFreezeType.setColors(detailColor,Color.black, Color.white, Color.black);

	pVersion = EZPopUpMenu(win, Rect(150, top+30+((index-1)*80), 150, 18), "Length version  ", 
		[
		'10:00' -> {arg a; version = 1; ("Track"+name+"version set to" + version).postln;},
		'5:30' -> {arg a; version = 2; ("Track"+name+"version set to" + version).postln;},
		],
		initVal: version - 1, labelWidth: 92);
	pVersion.font_(Font("Helvetica",12));
	pVersion.setColors(detailColor,Color.black, Color.white, Color.black);

	nLength = EZNumber(win, Rect(310, top+30+((index-1)*80), 115, 18), "Length factor  ", ControlSpec(0.1,5,step:0.1), 
		action: {lengthFactor = nLength.value; ("Track"+name+"length factor set to"+lengthFactor).postln;},
		initVal: lengthFactor, labelWidth: 82);
	nLength.font_(Font("Helvetica",12));
	nLength.setColors(detailColor,Color.black, Color.white, Color.black);

	nPitch = EZNumber(win, Rect(435, top+30+((index-1)*80), 110, 18), "Pitch factor  ", ControlSpec(0.2,10,step:0.01), 
		action: {pitchFactor = nPitch.value; ("Track"+name+"pitch factor set to"+pitchFactor).postln;},
		initVal: pitchFactor, labelWidth: 72);
	nPitch.font_(Font("Helvetica",12));
	nPitch.setColors(detailColor,Color.black, Color.white, Color.black);

	bMute = SCButton(win, Rect(650, top+30+((index-1)*80), 40, 18));
	bMute.canFocus = false;
	bMute.states = [["Mute",Color.black,Color.white],["Mute",Color.white,Color.black]];
	bMute.action = {bMuteBus.set(bMute.value)}; 


	// ------- Initialize -------------------------------------------------------------

	sLevelBus.set(volInit);
	sLevelMainBus.set(1);
	nGainBus.set(gainFactor);
	bMuteBus.set(0);
	
	// prefix for postln information display
	(index-1).do({
		prefix = prefix + "             ";
		});

	if(guiOn==0, {
		sLevel.visible_(false);
		nGain.visible_(false);
		bStart.visible_(false);
		tOutput.visible_(false);
		lOutput.visible_(false);
		bMute.visible_(false);
		nPitch.visible_(false);
		nLength.visible_(false);
		pVersion.visible_(false);
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
	{if(bStart.value == 1, {bStart.valueAction_(0); bStart.enabled = true;})}.defer();
	}

level
	{
	arg endLevel, releaseTime;
	var currentLevel = sLevel.value.dbamp;
		
	var step = (currentLevel-endLevel)/(releaseTime*10);
	var levelTask;
	
	levelTask = Task.new({
		((releaseTime*10)+1).do({|i|
		{sLevel.valueAction_(
			(currentLevel-(step*i))
			.ampdb)}.defer();
		0.1.wait;
		});
		});
	levelTask.play;
	}

levelMain
	{
	arg mainLevel;
	sLevelMainBus.set(mainLevel.dbamp);
	}

}