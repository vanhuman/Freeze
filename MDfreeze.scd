// MDFreeze
// © Robert van Heumen 2013
//
// version 2.0 changes
//	removed ornament code as it obscures the concept
//	removed fadeAB logic
//	cleaning up a bit, also on MDFreeze.sc / created a new version MDFreeze2.sc
//
// MDFreeze is an algorithmic composition for 8 speakers with a length of approximately 13 minutes.
//
// MDFreeze deals with the perception of music in time. By taking snapshots of an existing piece of music at irregular intervals and freezing those snapshots, the regular flow of time is altered and a parallel version of the music is created. This process started as a futile attempt to capture a piece of music in a small number of frames, but of course fails utterly. Music is not about taking samples, but about what happens between the samples; about the transition from one sample to the other, differences in air pressure. In principal music is about the experience of sound in time and can never be caught in discrete snapshots. Countability versus the Continuum. And then: is this a ripoff? A remix? Or a new original work? This is for the listener to decide. For those of you needing a hint what the original music is: that also was created by changing the regular flow of time, editing live performances together into one whole; revolutionary at that time and place.
//
// MDFreeze will be delivered in a standalone app with a size of approx. 211MB. To run MDFreeze you need a Mac computer with OS X 10.6.8 or higher and an 8-channel audio interface. Make sure that this audio interface is the default audio interface (System Preferences > Sound > Output) and make sure the device is switched on before you run the program. You could potentially also play the composition in stereo, hence you could use the internal audio from the computer.
//
// Start the composition by clicking on the big button that says 'Start MDFreeze'. It will take about 10 seconds before any sound is heard.
//
// Due to the nature of the work, you cannot pause it, nor start somewhere in the middle. If you experience any problems during the startup or playback of the piece, please contact robert@hardhatarea.com and include the post window data (click inside the window titled 'post', hit apple-A to select all, then copy and paste it into the email).

(
s.waitForBoot({

	// ------- Variables ---------------------------

	var guiOn = 1; // if 0 then simple GUI for playback
	var version = "2.1";
	var numChannels = 4; // specify number of channels, should be either 4, 8 or 12
	var subGain = 0.5; // gain factor for sub output
	var projectBase = Document.dir ++ "Freeze/Data/";
	var projectBase_standalone = String.scDir ++ "/MDFreeze/";
	var sampleFile = "1 Pharaoh's Dance.aiff";
	var detailColor = Color.new255(231,185,106);
	var penColorGrey = Color.grey(rrand(0,0.5), rrand(0,150));
	var penColorYellow = Color.new255(231,185,106, rrand(0,150));
	var top = 630;

	var outputFreeze = (), subOutChannel;
	var wMD, bStartMDF, tRecord, cRecord, pRecord, tChannels, cOut, tOut, sLevelMain, backBox, drawDone = 0, backDrop;
	var sLevelMainBus = Bus.control(s,1);
	var freezeBuf, freeze = (), tMD, fMD, fMDsub, output = Group.new;
	var fClock, lClock, tClock, testBuf, testSynth, bTest;

	// output mapping
	case
	{numChannels == 4}
		{
		"4-channel version".postln;
		outputFreeze = ( // initial output values for 4-channel
			A: "0,1", 		// L R
			B: "3,2", 		// RS LS
			C: "0,1,2,3",	// L R RS LS
			D: "0,1,2,3",	// L R RS LS
			E: "2,3",		// LS RS
			F: "1,0"		// R L
			);
		subOutChannel = 4; // sub output channel -- disable by sending to a not-connected channel (duh)
		}
	{numChannels == 8}
		{
		"8-channel version".postln;
		outputFreeze = ( // initial output values for 8-channel
			A: "0,1", 		// bottom L R
			B: "2,3", 		// bottom RS LS
			C: "0,1,2,3",	// bottom L R RS LS
			D: "4,5,6,7",	// top L R RS LS
			E: "4,5",		// top LS RS
			F: "6,7"		// top R L
			);
		subOutChannel = 8;  // sub output channel -- disable by sending to a not-connected channel (duh)
		}
	{numChannels == 12}
		{
		"12-channel version".postln;
		outputFreeze = ( // initial output values for 12-channel / bottom hexagon 0-5, top hexagon 6-11
			A: "0,1,4,3", 		// bottom 4 opposites
			B: "8,9,6,11", 		// upper 4 opposites turned 90deg as opposed to A
			C: "6,7,8,9,10,11",	// upper 6
			D: "0,1,2,3,4,5",		// bottom 6
			E: "7,10,8,9",		// upper 4 one side
			F: "2,5,3,4"		// bottom 4 one side
			);
		subOutChannel = 12;  // sub output channel -- disable by sending to a not-connected channel (duh)
		};

	// if we're in standalone version, change projectBase var
	if(~standalone == 1, {projectBase = projectBase_standalone; "Standalone version".postln;}, {"SC version".postln;});

	// read buffers now the projectBase is set
	freezeBuf = Buffer.read(s, projectBase++sampleFile);
	testBuf = Buffer.read(s, projectBase++"speakertest.aif");

	// ------- SynthDefs ---------------------------

	SynthDef(\speakerTest, {
		arg buf, gate, volMainBus;
		var sig, out;
		sig = PlayBuf.ar(1, buf, BufRateScale.kr(buf), loop: 1, doneAction: 2) *
				EnvGen.kr(Env.adsr(0,0,1,0.1), gate, doneAction: 2);
		sig = sig * In.kr(volMainBus,1);
		out = PulseCount.kr(Impulse.kr(1/2), Impulse.kr(1/(2*numChannels+2)));
//		out = PulseCount.kr(Impulse.kr(1/2), Impulse.kr(1/26));
		Out.ar(out, sig);
	}).send(s);

	s.sync; // wait for the SynthDefs to be sent


	// ------- Functions ---------------------------

	fMDsub = {
		arg letter, mode, value1, value2;
		case
			{mode=="start"} {freeze[letter].start}
			{mode=="stop"} {freeze[letter].stop}
			{mode=="levelRamp"} {freeze[letter].levelRamp(value1, value2)}
			{mode=="levelMain"} {freeze[letter].levelMain(value1)}
			;
		};

	fMD = {
		1.do({
			fMDsub.value('D',"start"); // low freq fade in out
			20.wait;
			fMDsub.value('E',"start"); // high freq fade in out
			15.wait;
			fMDsub.value('C',"start"); // original pitch fade in out
			25.wait;
			fMDsub.value('F',"start"); // thumb
			60.wait;
			fMDsub.value('A',"start"); // original freeze
			fMDsub.value('B',"start"); // original freeze a bit detuned
			(9.6*60).wait;
			fMDsub.value('A',"levelRamp",0.5,30);
			fMDsub.value('B',"levelRamp",0.5,30);
			(1.4*60).wait;
			10.wait; // just to be sure
			{bStartMDF.valueAction_(0)}.defer();
		});
		};

	fClock = {
		var init = 0;
		(13.15*60).do({|i|
			{lClock.string = "  " ++ (i+init).asTimeStringHM}.defer();
			1.wait;
			});
		};


	// ------- GUI ---------------------------

	wMD = Window("MDFreeze   |   version"+version+"("++numChannels++".1 channels)"+"  |   Robert van Heumen 2013",
				Rect(20,if(guiOn==1,{220},{600}),720,if(guiOn==1,{650},{170})), false)
	.background = Color.grey(0.8)
	.alpha = 0.9;

	backDrop = UserView(wMD,Rect(0,0,720,if(guiOn==1,{650},{170})))
	.mouseDownAction_({arg me; drawDone = 0; me.refresh});
	backDrop.drawFunc = {
		if(drawDone == 0, {
			rrand(40,100).do{|i|
				Pen.color = [penColorGrey,penColorYellow].wchoose([0.9,0.1]);
				Pen.addRect(
					Rect((wMD.bounds.width).rand, -2, 20, (wMD.bounds.height + 4))
				);
				Pen.perform(\stroke);
			};
			drawDone = 1;
			})
		};
	backDrop.clearOnRefresh_(true);

	bStartMDF = SmoothButton(wMD, Rect(30, 30, 250, 90))
	.radius_(2)
	.border_(1);
	bStartMDF.canFocus = false;
	bStartMDF.states = [["Start MDFreeze",Color.black,Color.white],["Stop MDFreeze",Color.white,Color.black]];
	bStartMDF.action = {
		if(bStartMDF.value == 1,
			{ // start
			if(cOut.value==true, { // only if checked
				freeze[\A].setOutput("0,1");
				freeze[\B].setOutput("1,0");
				freeze[\C].setOutput("0,1");
				freeze[\D].setOutput("0,1");
				freeze[\E].setOutput("1,0");
				freeze[\F].setOutput("0,1");
				},
				{
				freeze[\A].setOutput(outputFreeze[\A]);
				freeze[\B].setOutput(outputFreeze[\B]);
				freeze[\C].setOutput(outputFreeze[\C]);
				freeze[\D].setOutput(outputFreeze[\D]);
				freeze[\E].setOutput(outputFreeze[\E]);
				freeze[\F].setOutput(outputFreeze[\F]);
				}
				);
			if(cRecord.value == true, {s.record});
			cRecord.enabled = false;
			cOut.enabled = false;
			pRecord.enabled = false;
			bTest.enabled = false;
			tMD = Task(fMD).play;
			tClock = Task(fClock).play;
			},
			{ // stop
			if(cRecord.value == true, {{s.stopRecording}.defer(5)});
			tMD.stop;
			tClock.stop;
			lClock.string = "  00:00";
			cRecord.enabled = true;
			cOut.enabled = true;
			pRecord.enabled = true;
			bTest.enabled = true;
			fMDsub.value('A',"stop");
			fMDsub.value('B',"stop");
			fMDsub.value('C',"stop");
			fMDsub.value('D',"stop");
			fMDsub.value('E',"stop");
			fMDsub.value('F',"stop");
			});
		};
	bStartMDF.font = Font("Helvetica",30);

	backBox = UserView(wMD, Rect(300,30,260,90))
		.background_(Color.grey(1,0.6))
		.canFocus_(false);

	cOut = CheckBox.new(wMD, Rect(310,40,14,14))
		.background_(Color.white)
		.canFocus_(false);

	tOut = StaticText(wMD, Rect(330, 38, 200, 20))
		.font_(Font("Helvetica",10))
		.string_("Stereo version");

	cRecord = CheckBox.new(wMD, Rect(310,60,14,14))
		.background_(Color.white)
		.canFocus_(false);

	tRecord = StaticText(wMD, Rect(330, 58, 60, 20))
		.font_(Font("Helvetica",10))
		.string_("Record");

	pRecord = PopUpMenu(wMD, Rect(375, 60, 35, 16))
		.items_(["2","4","6","8"])
		.font_(Font("Helvetica",10))
		.action_({
			s.recChannels_(pRecord.item.asInt); ("NumChannels for recording set to"+pRecord.item).postln;
			});

	tChannels = StaticText(wMD, Rect(415, 58, 80, 20))
		.font_(Font("Helvetica",10))
		.string_("channels");

	bTest = SmoothButton(wMD, Rect(470, 40, 80, 20))
	.radius_(2)
	.border_(1);
	bTest.canFocus = false;
	bTest.states = [["SpeakerTest",Color.black,Color.white],["SpeakerTest",Color.white,Color.black]];
	bTest.font = Font("Helvetica",10);
	bTest.action = {
		if(bTest.value == 1,
			{testSynth = Synth(\speakerTest, [\gate,1,\buf,testBuf,\volMainBus,sLevelMainBus.index], target: output)},
			{testSynth.set(\gate,0)});
		};

	sLevelMain = EZSlider(wMD, Rect(310, 90, 250, 18), "Main level ",
			ControlSpec(0.ampdb, 4.ampdb, \db, units: "dB", step: 0.01),
			numberWidth: 45, layout: \horz, unitWidth: 25, initVal: 0, labelWidth: 60,
			action: {
				fMDsub.value('A',"levelMain",sLevelMain.value);
				fMDsub.value('B',"levelMain",sLevelMain.value);
				fMDsub.value('C',"levelMain",sLevelMain.value);
				fMDsub.value('D',"levelMain",sLevelMain.value);
				fMDsub.value('E',"levelMain",sLevelMain.value);
				fMDsub.value('F',"levelMain",sLevelMain.value);
				sLevelMainBus.set(sLevelMain.value.dbamp);
	}).font_(Font("Helvetica",10));

	lClock = StaticText(wMD, Rect(580,30,110,90))
		.font_(Font("Helvetica",30))
		.string_("  00:00")
		.background_(Color.white);

	freeze.put(\A, MDFreeze2.new("A", index: 1, buf: freezeBuf, version: 2, win: wMD, guiOn: guiOn,
		 		lengthFactor: 2, output: outputFreeze[\A].split($,), envLPFCurve: 1, freezeType: 1,
		 		pitchFactor: 1, gainFactor: -3.dbamp, wait: 2, subOut: subOutChannel));
	freeze.put(\B, MDFreeze2.new("B", index: 2, buf: freezeBuf, version: 2, win: wMD, guiOn: guiOn,
				lengthFactor: 2, output: outputFreeze[\B].split($,), envLPFCurve: 1, freezeType: 1,
				pitchFactor: 1.03, gainFactor: -3.dbamp, subOut: subOutChannel));
	freeze.put(\C, MDFreeze2.new("C", index: 3, buf: freezeBuf, version: 1, win: wMD, guiOn: guiOn,
				lengthFactor: 1, output: outputFreeze[\C].split($,), envLPFCurve: 1, freezeType: 2,
				pitchFactor: 0.97, gainFactor: -3.dbamp, subOut: subOutChannel));
	freeze.put(\D, MDFreeze2.new("D", index: 4, buf: freezeBuf, version: 1, win: wMD, guiOn: guiOn,
				lengthFactor: 1, output: outputFreeze[\D].split($,), envLPFCurve: 1, freezeType: 2,
				pitchFactor: 0.5, gainFactor: 6.5.dbamp, wait: 2, subOut: subOutChannel));
	freeze.put(\E, MDFreeze2.new("E", index: 5, buf: freezeBuf, version: 1, win: wMD, guiOn: guiOn,
				lengthFactor: 1, output: outputFreeze[\E].split($,), envLPFCurve: 1, freezeType: 2,
				pitchFactor: 6, gainFactor: -9.dbamp, wait: 2, subOut: subOutChannel));
	freeze.put(\F, MDFreeze2.new("F", index: 6, buf: freezeBuf, version: 2, win: wMD, guiOn: guiOn,
				lengthFactor: 2, output: outputFreeze[\F].split($,), envLPFCurve: 1, freezeType: 4,
				pitchFactor: 1, gainFactor: -6.dbamp, wait: 2, subOut: subOutChannel));

	wMD.onClose_({
		// stop all
		fMDsub.value('A',"stop");
		fMDsub.value('B',"stop");
		fMDsub.value('C',"stop");
		fMDsub.value('D',"stop");
		fMDsub.value('E',"stop");
		fMDsub.value('F',"stop");
		freezeBuf.free;
		testBuf.free;
		output.free;
		tMD.stop;
		tClock.stop;
		if(~wMeterAvail.notNil, {~wMeter.close});
		});


	// ------- Initialize -------------------------------------------------------------

	sLevelMainBus.set(1);

	if(guiOn==0, {
		cRecord.visible_(false);
		tChannels.visible_(false);
		pRecord.visible_(false);
		tRecord.visible_(false);
		});

	w = Window("dummy").front; w.close;

	// show meter
	s.meter();

	// show window
	wMD.front;

});
)

