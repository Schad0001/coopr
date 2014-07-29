require 'simplecov'
SimpleCov.start do
  add_filter '/spec/'
end

require 'rspec/autorun'

require_relative '../automator'
require_relative '../pluginmanager'
require_relative '../provider'
require_relative '../utils'
