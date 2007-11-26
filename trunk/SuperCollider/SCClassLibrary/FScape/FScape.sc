/**
 *	Client-side represenation of the FScape audio renderer.
 *
 *	@version	0.11, 25-Jun-06
 *	@author	Hanns Holger Rutz
 */
FScape {
	classvar <>local, <>default, uniqueID, <>timeoutClock;
	var		<name, <addr, <isLocal;

	*initClass {
		uniqueID		= 0;
		default		= local = FScape.new( \localhost, NetAddr( "127.0.0.1", 0x4653 ));
		timeoutClock	= AppClock;
	}

	*new { arg name, addr;
		^super.new.prInitFScape( name, addr );
	}
	
	prInitFScape { arg argName, argAddr;
		name = argName;
		addr = argAddr;
		if (addr.isNil, { addr = NetAddr( "127.0.0.1", 0x4653 ); });
		isLocal = addr.addr == 2130706433;
	}
	
	/**
	 *	@warning	asynchronous; needs to be called inside a Routine
	 */
	initTree {
	}
	
	// !experimental!
	closeAllDocuments { arg doneAction;
		fork {
			var msg, num;
			msg = this.query( '/doc', \count );
			if( msg.notNil, {
				num = msg[ 0 ];
	//			("# of Docs = "++num).inform;
				num.do({ arg idx;
					this.sendMsg( '/doc/index/0', \close );
				});
				doneAction.value( true );
			}, {
				doneAction.value( false );
			});
		};
	}

	// !experimental!
	openAndProcess { arg docFile, visible = true, closeWhenDone = true, doneAction, progFunc, progInterval = 1;
		var msg, num, isRunning, docID, addr, error;
		
		fork {
			this.sendMsg( '/doc', \open, docFile, visible );
			msg = this.query( '/doc', \count );
			if( msg.notNil, {
				block { arg break;
					num = msg[ 0 ];
					num.do({ arg idx;
						msg = this.query( "/doc/index/" ++ idx, [ \id, \file, \running ]);
						if( msg.notNil, {
							if( msg[ 1 ].asString == docFile, {
								docID	= msg[ 0 ];
								isRunning	= msg[ 2 ] != 0;
								break.value;
							});
						});
					});
				};
				if( docID.notNil, {
					addr = "/doc/id/" ++ docID;
					while({ isRunning }, {
						this.sendMsg( addr, \stop );
						1.wait;
						msg = this.query( addr, \running );
						if( msg.notNil, {
							isRunning = msg[ 0 ] != 0;
						}, {
							docID	= nil;
							isRunning	= false;
						});
					});
				}, {
					error = 'fileNotFound';
				});
				if( docID.notNil, {
					this.sendMsg( addr, \start );
						
					isRunning = true;
						
					1.wait;	// tricky sync
						
					while({ isRunning }, {
						progInterval.wait;
						msg = this.query( addr, [ \running, \progression, \error ]);
						if( msg.notNil, {
							isRunning = msg[ 0 ] != 0;
							progFunc.value( msg[ 1 ]);
							if( isRunning.not, {
								if( closeWhenDone, { this.sendMsg( addr, \close ); });
								doneAction.value( msg[ 1 ], if( msg[ 2 ].asString == "", nil, msg[ 2 ]));
							});
						}, {
							"timeout".warn;
							isRunning = false;
						});
					});
				});
			});		
			if( docID.isNil, { doneAction.value( 0, error ?? 'timeout');});
		};
	}
	
	sendMsg { arg ... msg;
		addr.sendMsg( *msg );
	}
	
	sendBundle { arg time ... msgs;
		addr.sendBundle( time, *msgs );
	}

	queryFunc { arg func, path, properties, timeout = 4.0, condition;
		Routine {
			var result;
			result = this.query( path, properties, timeout, condition );
			func.value( result );
		}.play;
	}

	/**
	 *	@warning	asynchronous; needs to be called inside a Routine
	 */
	get { arg path, getArgs, timeout = 4.0, condition;
		^this.prQuery( path, \get, '/get.reply', getArgs, timeout, condition );
	}

	/**
	 *	@warning	asynchronous; needs to be called inside a Routine
	 */
	query { arg path, properties, timeout = 4.0, condition;
		^this.prQuery( path, \query, '/query.reply', properties, timeout, condition );
	}

	/**
	 *	@warning	asynchronous; needs to be called inside a Routine
	 */
	prQuery { arg path, sendCmd, replyCmd, msgArgs, timeout = 4.0, condition;
		var resp, id, result, cancel;
		
		if( condition.isNil, { condition = Condition.new; });
		
		id			= this.nextUniqueID;
		msgArgs		= msgArgs.asArray;
		
// OSCpathResponder seems to be broken
//		resp = OSCpathResponder( addr, [ "/query.reply", id ], {
//			arg time, resp, msg;
//
//msg.postln;
//			
//			resp.remove;
////			condition.test = true;
////			condition.signal;
//			doneFunc.value( path, msg.copyToEnd( 2 ));
//		});
		resp = OSCresponderNode( addr, replyCmd, {
			arg time, resp, msg;
			
			if( msg[ 1 ] == id, {
				if( cancel.notNil, {Êcancel.stop; });
				resp.remove;
				result			= msg.copyToEnd( 2 );
				condition.test	= true;
				condition.signal;
//				doneFunc.value( path, msg.copyToEnd( 2 ));
			});
		});
		resp.add;
		condition.test = false;
		if( timeout > 0.0, {
			cancel = Task({
				timeout.wait;
				resp.remove;
				result			= nil;
				condition.test	= true;
				condition.signal;
			
			}, timeoutClock );
			cancel.start;
		});
		addr.sendMsg( path, sendCmd, id, *msgArgs );
		condition.wait;
		^result;
	}
	
	nextUniqueID {
		uniqueID = uniqueID + 1;
		^uniqueID;
	}

	listSendMsg { arg msg;
		addr.sendMsg( *msg );
	}
	
 	listSendBundle { arg time, msgs;
		addr.sendBundle( time, *msgs );
	}

	/**
	 *	@warning	asynchronous; needs to be called inside a Routine
	 */
	sendMsgSync { arg path, cmd, msgArgs, timeout = 4.0, condition;
		var respDone, respFailed, cancel, result;

		if( condition.isNil ) { condition = Condition.new; };

		respDone	= OSCresponderNode( addr, '/done', { arg time, resp, msg;
			if( msg[ 1 ].asSymbol === path.asSymbol and: {Êmsg[ 2 ].asSymbol === cmd.asSymbol }) {
				if( cancel.notNil, {Êcancel.stop; });
				resp.remove;
				result			= msg.copyToEnd( 3 );
				condition.test	= true;
				condition.signal;
			};
		});
		respFailed = OSCresponderNode( addr, '/failed', { arg time, resp, msg;
			if( msg[ 1 ].asSymbol === path.asSymbol and: {Êmsg[ 2 ].asSymbol === cmd.asSymbol }) {
				if( cancel.notNil, {Êcancel.stop; });
				resp.remove;
				result			= nil;
				condition.test	= true;
				condition.signal;
			};
		});
		respDone.add;
		respFailed.add;
		condition.test = false;
		if( timeout > 0.0, {
			cancel = Task({
				timeout.wait;
				respDone.remove;
				respFailed.remove;
				result			= false;
				condition.test	= true;
				condition.signal;
			
			}, timeoutClock );
			cancel.start;
		});
		condition.test = false;
		addr.sendMsg( path, cmd, *msgArgs );
		condition.wait;
		^result;
	}

	/**
	 *	@warning	asynchronous; needs to be called inside a Routine
	 */
	sync { arg bundles, latency, timeout = 4.0, condition; // array of bundles that cause async action
		var resp, id, cancel, result;
		
		if( condition.isNil, { condition = Condition.new; });
		
		id = UniqueID.next;
		resp = OSCresponderNode( addr, '/synced', { arg time, resp, msg;
			if( msg[ 1 ] == id, {
				if( cancel.notNil, {Êcancel.stop; });
				resp.remove;
				result			= true;
				condition.test 	= true;
				condition.signal;
			});
		});
		resp.add;
		condition.test = false;
		if( timeout > 0.0, {
			cancel = Task({
				timeout.wait;
				resp.remove;
				result			= false;
				condition.test	= true;
				condition.signal;
			
			}, timeoutClock );
			cancel.start;
		});
		if( bundles.isNil, {
			addr.sendBundle( latency, [ '/sync', id ]);
		}, {
			addr.sendBundle( latency, *(bundles ++ [[ '/sync', id ]]));
		});
		condition.wait;
		^result;
	}

	ping { arg n = 1, wait = 0.1, func;
		var result = 0, pingFunc;
		
//		if( serverRunning.not ) { "server not running".postln; ^this };
		pingFunc = {
			Routine.run {
				var t, dt;
				t = Main.elapsedTime;
				this.sync;
				dt = Main.elapsedTime - t;
				("measured latency:" + dt + "s").postln;
				result = max( result, dt );
				n = n - 1;
				if( n > 0, { 
					SystemClock.sched( wait, { pingFunc.value; nil; });
				}, {
					("maximum determined latency of" + name + ":" + result + "s").postln;
					func.value( result );
				});
			};
		};
		pingFunc.value;
	}

	dumpOSC { arg code = 1, outgoing;
		/*
			0 - turn dumping OFF.
			1 - print the parsed contents of the message.
			2 - print the contents in hexadecimal.
			3 - print both the parsed and hexadecimal representations of the contents.
		*/
//		dumpMode = code;
		if( outgoing.isNil, {
			this.sendMsg( '/dumpOSC', code );
		}, {
			this.sendMsg( '/dumpOSC', code, outgoing );
		});
	}
}

// XXX this is the old properties format which is likely to change!!
FScapeDoc {
	var <map;
	var readFile;
	
	*new {Êarg map;
		^super.new.prInitDoc( map );
	}
	
	prInitDoc { arg argMap;
		map = argMap ?? {ÊIdentityDictionary.new };
	}
	
	at {Êarg key;
		^map[ key ];
	}
	
	put { arg key, value;
		^map.put( key, value );
	}

	*read {Êarg file;
		var f;
		
		f = FScapeDoc.new;
		if( f.read( file ), {
			^f;
		}, {
			^nil;
		});
	}
	
	// Format:
	//#Created by FScape; do not edit manually!
	//#Sun Jun 25 19:58:54 CEST 2006
	//<key>=<value>
	//...
	
	read { arg fileName;
		var file, line, idx;
		readFile	= fileName;
		map.clear;
		try {
			file = File( fileName, "r" );
			if( file.getLine.beginsWith( "#Created by FScape" ).not, {
				Error( fileName.asString ++ " : not an FScape document" ).throw;
			});
			while({ file.pos < file.length }, {
				line = file.getLine;
				if( line.beginsWith( $# ).not, {
					idx = line.indexOf( $= );
					if( idx.notNil, {
						map.put( line.copyFromStart( idx - 1 ).asSymbol, line.copyToEnd( idx + 1 ));
					});
				});
			});
			file.close;
			^true;
		}
		{ arg error;
			error.reportError;
			^false;
		};
	}
	
	write { arg fileName;
		var file;
		fileName = fileName ?? readFile;
		
		try {
			file = File( fileName, "w" );
			file.putString( "#Created by FScape; do not edit manually!\n#" ++
				Date.getDate.asString ++ "\n" );
			map.keysValuesDo({ arg key, value;
				file.putString( key.asString ++ "=" ++ value.asString ++ "\n" );
			});
			file.close;
			^true;
		}
		{ arg error;
			error.reportError;
			^false;
		};
	}
}