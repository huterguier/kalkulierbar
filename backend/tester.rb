require 'net/http'
require 'json'
require 'date'

=begin
	How to write a system test scenario

	1. Create a new function taking a TestRequest instance as an argument
	2. Log some output (What are you testing right now?) using logMsg
	3. Generate the test data you need (use ClauseGenerator for random clause sets)
	4. Send & validate some requests using TestRequest.post
	4.1 For simlpe error tests, setting the expected status code should be enough
	    You might want to check the error message, too.
	4.2 If you know exactly what the response should be, pass the response as a string
	    to post() in the requestValidator parameter
	4.3 If you only care about a part of the response, try passing a regular expression
	    that matches what you want
	4.4 For anything more complex, pass a Lambda function taking the response body and
	    returning a boolean
	5. Log success or failure using logSuccess and logError - give some stats, too, if
	   you like
	6. Call your function with the trq object and appropriate parameters at the bottom
	   of this file
	7. Done!
=end

def logMsg(msg, c = "0")
	STDERR.puts "[#{Time.now.strftime('%d/%m %H:%M:%S')}] \e[#{c}m#{msg}\e[0m"
end

def logSuccess(msg)
	logMsg(msg, "32") # Green font color
end

def logError(msg)
	logMsg(msg, "41") # Red font color
end

# Network request helper class
class TestRequest

	# Open a connection to the specified host
	def initialize(host = 'localhost', port = 7000, headers = 'default')
		@host = host
		@port = port
		@headers = headers
		@net = Net::HTTP.new(host, port)
		#@net.use_ssl = true
		@net.start()

		logMsg "Targeting endpoint #{@host}:#{@port}, #{@headers} headers"
	end

	# Get the response for a request without performing any validation
	# data is post data to send in form encoding
	# i.e. "formula=a,b;c&something=somethingelse"
	def getPostResponse(path, data, debug = false, silent = false)
		uri = URI("http://#{@host}#{path}")

		req = Net::HTTP::Post.new(uri, initheader = getHeaders)
		
		req.body = data

		logMsg "Sending request to #{path}" if debug

		# Catch and log network issues
		begin
			response = @net.request(req)
		rescue Timeout::Error, Errno::EINVAL, Errno::ECONNRESET, EOFError, Net::HTTPBadResponse, Net::HTTPHeaderSyntaxError, Net::ProtocolError => e
			if !silent
				logMsg "Network Error: #{e.to_s}\n---------------- request body: #{data}"
			end
			return nil
		end

		# Catch and log unexpected status code
		if response.code != "200"
			if !silent
				logMsg("Unexpected HTTP status: #{response.code.to_s} - #{response.msg}\n---------------- response: #{response.body}\n---------------- request body: #{data}")
			end
			return nil
		end

		return response.body
	end

	# Send a request and validate response
	# data is post data to send, form encoded
	# responseValidator is either:
	# - a String which the response body has to match exactly
	# - a RegExp which the response body has to match
	# - a Lambda which, when invoked with the response body as an argument, has to return true
	def post(path, data, responseValidator, expectedStatus = '200', debug = false)
		uri = URI("http://#{@host}#{path}")

		req = Net::HTTP::Post.new(uri, initheader = getHeaders)
		
		req.body = data

		logMsg "Sending request to #{path}" if debug

		# Catch network issues
		begin
			response = @net.request(req)
		rescue Timeout::Error, Errno::EINVAL, Errno::ECONNRESET, EOFError, Net::HTTPBadResponse, Net::HTTPHeaderSyntaxError, Net::ProtocolError => e
			logMsg "Network Error: #{e.to_s}\n---------------- request body: #{data}"
			return false
		end

		# Catch unexpected status
		if response.code != expectedStatus.to_s
			logMsg("Unexpected HTTP status: #{response.code.to_s} - #{response.msg}\n---------------- response: #{response.body}\n---------------- request body: #{data}")
			return false
		end

		# Perform response validation
		if (responseValidator.is_a? String and response.body == responseValidator) or (responseValidator.is_a? Regexp and response.body =~ responseValidator)
			logMsg "Request OK" if debug
			return true
		elsif responseValidator.is_a? Proc and responseValidator.call(response.body)
			logMsg "Request OK" if debug
			return true
		else
			logMsg("Response does not match expected format\n---------------- response: #{response.body}\n---------------- request body: #{data}")
			return false
		end

	end

	# Close connection to host
	def finish
		@net.finish
	end

	# Request headers to use (mostly unimportant)
	def getHeaders
		if @headers == 'default'
			{
				'Host' => @host,
				'Content-Type' => 'application/x-www-form-urlencoded',
				'Accept' => 'text/html,application/json;q=0.9,*/*;q=0.8',
				'Referer' => "http://#{@host}/",
				'Connection' => 'keep-alive',
				'Cache-Control' => 'max-age=0, no-cache',
				'Pragma' => 'no-cache',
				'Accept-Language' => 'en-US,en;q=0.5',
				'User-Agent' => 'Mozilla/5.0 (Windows NT 6.1; rv:60.0) Gecko/20100101 Firefox/60.0'
			}
		else
			{}
		end
	end
end

# Generate random ClauseSet formulas
class ClauseGenerator

	# Generate a mix of high-entropy and low-entropy variables
	def genVar
		if rand < 0.5
			length = (2 ** rand(0.0..4.0)).ceil
			charset = "abcdefghijklmnopqrstuvqxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".split('')
			(0...length).map{ charset.sample }.join
		else
			["a", "b", "c", "d"].sample
		end
	end

	# Negate some of the variables
	def genAtom(clause)
		res = rand() < 0.5 ? "!#{genVar()}" : genVar()
		clause.push(res)
		return res
	end

	# Pack a bunch of them together in a clause
	def genDisjunction(set)
		clause = []
		length = (2 ** rand(0.0..4.0)).ceil
		res = (0...length).map{ genAtom(clause) }.join(',')
		set.push(clause)
		return res
	end

	# Pack some clauses together
	# set is an array to hold a more machine-readable representation
	# of the clause set to check backend output again
	# Like this: [["a", "!b"], ["c"]]
	# return value is the regular formula string
	def genClauseSet(set = [])
		length = (2 ** rand(0.0..5.0)).ceil
		(0...length).map{ genDisjunction(set) }.join(';')
	end
end

def fuzzClauseParsing(trq, count = 100)
	logMsg "Fuzzing valid formulas"
	success = true
	cg = ClauseGenerator.new

	count.times() {
		clauses = []
		string = cg.genClauseSet(clauses)
		success &= trq.post('/prop-tableaux/parse', "formula=#{string}", ->(res){ checkEquiv(JSON.parse(res)['clauseSet']['clauses'], clauses)}, 200)
	}

	if success
		logSuccess "Test successful - sent #{count.to_s} requests"
	else
		logError "Test failed!"
	end
end

# Checks if a clauseSet received is equivalent to a clauseSet obtained from ClauseGenerator
def checkEquiv(got, expect)
	return false unless got.length === expect.length
	expect.each_with_index { |eClause, i|
		return false unless eClause.length === got[i]['atoms'].length
		gClause = got[i]['atoms'].map{ |a| (a['negated'] ? "!" : "") + a['lit']}
		
		# Elements in got or expected, but not the intersection of got and expected
		differring = (eClause | gClause) - (eClause & gClause)
		return false if differring.length > 0
	}
	true
end

def testInvalidParam(trq)
	cg = ClauseGenerator.new
	logMsg "Testing invalid formulas"
	formulas = ["", ",", "a,", "a,b;", "a,b;c,", "a,b;c,;e", ",b;c,;e", ";c,;e"]
	success = true

	success &= trq.post('/prop-tableaux/parse', "formul=#{cg.genClauseSet()}", /parameter.*needs to be present/i, 400)

	formulas.each { |f|
		success &= trq.post('/prop-tableaux/parse', "formula=#{f}", /invalid input formula format/i, 400)
	}
	
	if success
		logSuccess "Test successful - sent #{(formulas.length + 1).to_s} requests"
	else
		logError "Test failed!"
	end
end

def testRootNodeCreation(trq, count = 20)
	logMsg "Testing correct root node creation"

	success = true
	cg = ClauseGenerator.new

	validate = ->(res) {
		r = JSON.parse(res)
		valid = r['nodes'].length === 1
		n = r['nodes'][0]
		valid &= n['spelling'] === 'true'
		valid &= !n['isClosed']
		valid &= !n['negated']
		valid &= n['parent'] === nil
		valid &= n['children'].length === 0
		valid
	}

	count.times() {
		string = cg.genClauseSet()
		success &= trq.post('/prop-tableaux/parse', "formula=#{string}", validate, 200)
	}
	
	if success
		logSuccess "Test successful - sent #{count.to_s} requests"
	else
		logError "Test failed!"
	end
end

def testStateModification(trq, count = 50)
	logMsg "Testing tamper protection"
	cg = ClauseGenerator.new
	iterations = count / 10
	success = true

	iterations.times() {
		validState = trq.getPostResponse('/prop-tableaux/parse', "formula=#{cg.genClauseSet}")
		parsed = JSON.parse(validState)

		# Test unmodified state
		success &= trq.post('/prop-tableaux/close', "state=#{JSON.dump(parsed)}", "false", 200)

		10.times() {
			modified = JSON.parse(validState)
			modType = rand(1..2)
			case modType
			when 1
				tweakNode(modified)
			when 2
				tweakClause(modified)
			end

			success &= trq.post('/prop-tableaux/close', "state=#{JSON.dump(modified)}", /.*Invalid tamper protection seal.*/, 400)
		}
	}

	if success
		logSuccess "Test successful - sent #{(iterations * 11).to_s} requests"
	else
		logError "Test failed!"
	end
end

# Internal function, irrelevant
def tweakNode(state)
	i = rand(0...state['nodes'].length)
	case rand(1..4)
	when 1
		state['nodes'][i]['negated'] = !state['nodes'][i]['negated']
	when 2
		state['nodes'][i]['isClosed'] = !state['nodes'][i]['isClosed']
	when 3
		state['nodes'][i]['parent'] = (state['nodes'][i]['parent'] == nil ? rand(1..500) : state['nodes'][i]['parent'] + rand(1..500))
	when 4
		state['nodes'][i]['children'].push(rand(0..500))
	end
end

# Internal function, irrelevant
def tweakClause(state)
	i = rand(0...state['clauseSet']['clauses'].length)
	j = rand(0...state['clauseSet']['clauses'][i]['atoms'].length)
	state['clauseSet']['clauses'][i]['atoms'][j]['negated'] = !state['clauseSet']['clauses'][i]['atoms'][j]['negated']
end

def tryCloseUncloseable(trq, iterations = 13, verbose = false)
	logMsg "Trying to close unclosable proof"
	
	if bogoATP(trq, "b,c;!a,b;!c;!b,!c", "UNCONNECTED", false, iterations, verbose)
		logError "Test failed"
	else
		logSuccess "Test successful"
	end
end

def tryCloseTrivial(trq, iterations = 5, verbose = false)
	logMsg "Trying to close a trivial proof"
	
	if bogoATP(trq, "a,b;!a;!b", "UNCONNECTED", false, iterations, verbose)
		logSuccess "Test successful"
	else
		logError "Test failed"
	end
end

def tryCloseCloseable(trq, iterations = 15, verbose = false)
	logMsg "Trying to close a proof"
	
	if bogoATP(trq, "a,b,c;a,!b;!a;!c", "UNCONNECTED", false, iterations, verbose)
		logSuccess "Test successful"
	else
		logError "Test failed"
	end
end

def tryCloseConnected(trq, iterations = 15, verbose = false)
	logMsg "Trying to close a strongly connected proof"
	
	if bogoATP(trq, "a,b,c,d,e,f;a,!b;!a;!c;d,!e;!d;!f", "STRONGLYCONNECTED", false, iterations, verbose)
		logSuccess "Test successful"
	else
		logError "Test failed"
	end
end

def tryCloseRegular(trq, iterations = 15, verbose = false)
	logMsg "Trying to close a regular proof"
	
	if bogoATP(trq, "a,b,c,d,e,f;a,!b;!a;!c;d,!e;!d;!f", "UNCONNECTED", true, iterations, verbose)
		logSuccess "Test successful"
	else
		logError "Test failed"
	end
end

def bogoATP(trq, formula, connectedness = "UNCONNECTED", regular = false, iterations = 10, verbose = false)
	rqcount = 1
	moves = 0
	state = trq.getPostResponse('/prop-tableaux/parse', "formula=#{formula}&params={\"type\":\"#{connectedness.to_s}\",\"regular\":#{regular.to_s}}")
	parsed = JSON.parse(state)

	# Apply all single-literal clauses to improve closability and control tree fanout
	if connectedness == "UNCONNECTED"
		lid = 0
		parsed['clauseSet']['clauses'].each_with_index { |c,i|
			if c['atoms'].length == 1
				logMsg "Pre-expanding clause #{i.to_s} on node #{lid.to_s}" if verbose
				state = trq.getPostResponse('/prop-tableaux/move', "state=#{state}&move={type:\"e\",id1:#{lid.to_s},id2:#{i.to_s}}")
				lid += 1
				rqcount += 1
				moves += 1
			end
		}
	end

	parsed = JSON.parse(state)

	# BogoATP loop, close everything closeable, then expand a random leaf
	iterations.times() {

		# Try closing all leaves
		parsed['nodes'].each_with_index { |n, i|
			if n['children'].length == 0 && !n['isClosed']
				# Brute-force close with every available ID
				parsed['nodes'].length.times() { |j|
					newstate = trq.getPostResponse('/prop-tableaux/move', "state=#{state}&move={type:\"c\",id1:#{i.to_s},id2:#{j.to_s}}", false, true)
					rqcount += 1

					if newstate != nil
						logMsg "Closed node #{n['negated'] ? "!" : ""}#{n['spelling']} with node ID #{j.to_s}" if verbose
						moves += 1
						state = newstate
						break
					end
				}
			end
		}

		# Try closing the proof
		logMsg "Trying to close proof" if verbose
		if !trq.post('/prop-tableaux/close', "state=#{state}", "false", 200)
			logMsg "BogoATP Proof closed - #{rqcount.to_s} requests sent / #{moves.to_s} moves applied"
			return true
		end

		rqcount += 1
		parsed = JSON.parse(state)

		# Try expanding a leaf
		leaves = parsed['nodes'].each_with_index.filter{ |n,i| n['children'].length == 0 && !n['isClosed'] }.map { |x,i| i }.shuffle
		clauses = (0...parsed['clauseSet']['clauses'].length).to_a.shuffle
		leaves.each { |l|
			clauses.each { |c|
				newstate = trq.getPostResponse('/prop-tableaux/move', "state=#{state}&move={type:\"e\",id1:#{l.to_s},id2:#{c.to_s}}", false, true)
				rqcount += 1
				if newstate != nil
					logMsg "Expanded leaf #{l.to_s} with clause #{c.to_s}" if verbose
					state = newstate
					moves += 1
					break
				end
			}
		}

		parsed = JSON.parse(state)

		# I love not having some tool tell me how long my lines can be
		# Isn't it beautiful?
		logMsg parsed['nodes'].map { |e| "(#{e['negated'] ? "!" : ""}#{e['spelling']}|#{e['parent'].to_s}|#{e['isClosed'] || e['children'].length > 0 ? "c" : "o"})" }.join(', ') if verbose
	}

	logMsg "BogoATP Proof search unsuccessful - #{rqcount.to_s} requests sent / #{moves.to_s} moves applied"
	return false
end

trq = TestRequest.new

logMsg("Testing PropositionalTableaux")
testInvalidParam(trq)
fuzzClauseParsing(trq)
testRootNodeCreation(trq)
testStateModification(trq)
tryCloseTrivial(trq)
tryCloseCloseable(trq)
tryCloseConnected(trq)
tryCloseRegular(trq)
tryCloseUncloseable(trq)