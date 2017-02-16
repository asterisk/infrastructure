def call(branch, user, host, name, dsn) {

	stage("realtime-setup") {
		sh "psql --username=${user} --host=${host} --db=${name} --command='DROP OWNED BY ${user} CASCADE'"

		dir("testsuite") {
			writeFile file: "test-config.yaml", text: """\
			global-settings:
			    test-configuration: config-realtime
			
			    condition-definitions:
			            -
			                name: 'threads'
			                pre:
			                    typename: 'thread_test_condition.ThreadPreTestCondition'
			                post:
			                    typename: 'thread_test_condition.ThreadPostTestCondition'
			                    related-type: 'thread_test_condition.ThreadPreTestCondition'
			            -
			                name: 'sip-dialogs'
			                pre:
			                    typename: 'sip_dialog_test_condition.SipDialogPreTestCondition'
			                post:
			                    typename: 'sip_dialog_test_condition.SipDialogPostTestCondition'
			            -
			                name: 'locks'
			                pre:
			                    typename: 'lock_test_condition.LockTestCondition'
			                post:
			                    typename: 'lock_test_condition.LockTestCondition'
			            -
			                name: 'file-descriptors'
			                pre:
			                    typename: 'fd_test_condition.FdPreTestCondition'
			                post:
			                    typename: 'fd_test_condition.FdPostTestCondition'
			                    related-type: 'fd_test_condition.FdPreTestCondition'
			            -
			                name: 'channels'
			                pre:
			                    typename: 'channel_test_condition.ChannelTestCondition'
			                post:
			                    typename: 'channel_test_condition.ChannelTestCondition'
			            -
			                name: 'sip-channels'
			                pre:
			                    typename: 'sip_channel_test_condition.SipChannelTestCondition'
			                post:
			                    typename: 'sip_channel_test_condition.SipChannelTestCondition'
			            -
			                name: 'memory'
			                pre:
			                    typename: 'memory_test_condition.MemoryPreTestCondition'
			                post:
			                    typename: 'memory_test_condition.MemoryPostTestCondition'
			                    related-type: 'memory_test_condition.MemoryPreTestCondition'
			
			config-realtime:
			    test-modules:
			        modules:
			            -
			                typename: realtime_converter.RealtimeConverter
			                config-section: realtime-config
			
			    realtime-config:
			        username: "${user}"
			        host: "${host}"
			        db: "${name}"
			        dsn: "${dsn}"
			
		""".stripIndent()
		}				
		writeFile file: 'config.ini', text: """\
		[alembic]
		script_location = asterisk/contrib/ast-db-manage/config
		sqlalchemy.url = postgresql://${user}@${host}/${name}
		
		[loggers]
		keys = root,sqlalchemy,alembic
		
		[handlers]
		keys = console
		
		[formatters]
		keys = generic
		
		[logger_root]
		level = WARN
		handlers = console
		qualname =
		
		[logger_sqlalchemy]
		level = WARN
		handlers =
		qualname = sqlalchemy.engine
		
		[logger_alembic]
		level = INFO
		handlers =
		qualname = alembic
		
		[handler_console]
		class = StreamHandler
		args = (sys.stderr,)
		level = NOTSET
		formatter = generic
		
		[formatter_generic]
		format = %(levelname)-5.5s [%(name)s] %(message)s
		datefmt = %H:%M:%S
		
		""".stripIndent()

		echo "Creating database tables"
		sh "alembic -c config.ini upgrade head"
		sh "rm -rf config.ini"
	}
}