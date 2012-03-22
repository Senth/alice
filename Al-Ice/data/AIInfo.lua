--
--  Info Definition Table format
--
--
--  These keywords must be lowercase for LuaParser to read them.
--
--  key:      user defined or one of the SKIRMISH_AI_PROPERTY_* defines in
--            SSkirmishAILibrary.h
--  value:    the value of the property
--  desc:     the description (could be used as a tooltip)
--
--
--------------------------------------------------------------------------------
--------------------------------------------------------------------------------

local infos = {
	{
		key    = 'shortName',
		value  = 'AlIce',
		desc   = 'machine conform name.',
	},
	{
		key    = 'version',
		value  = '0.3', -- AI version - !This comment is used for parsing!
	},
	{
		key    = 'className',
		value  = 'alice.AIFactory',
		desc   = 'fully qualified name of a class that implements interface com.springrts.ai.AI',
	},
	{
		key    = 'name',
		value  = 'Al Ice',
		desc   = 'human readable name.',
	},
	{
		key   = 'description',
		value = 'Al Ice made by Senth and Kazzoa',
		desc  = 'description of the bot',
	},
	{
		key    = 'loadSupported',
		value  = 'no',
		desc   = 'whether this AI supports loading or not',
	},
	{
		key    = 'interfaceShortName',
		value  = 'Java', -- AI Interface name - !This comment is used for parsing!
		desc   = 'the shortName of the AI interface this AI needs',
	},
	{
		key    = 'interfaceVersion',
		value  = '0.1', -- AI Interface version - !This comment is used for parsing!
		desc   = 'the minimum version of the AI interface required by this AI',
	},
}

return infos
