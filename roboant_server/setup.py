from setuptools import setup

setup(name='roboant_server',
      version='0.1',
      description='Server for the RoboAnt project',
      url='http://github.com/d3kod/roboant',
      author='Aleksandar Kodzhabashev',
      author_email='akodzhabashev@gmail.com',
      license='MIT',
      packages=['roboant_server'],
      scripts=['bin/roboant_server'],
      install_requires=[
          'pygame',
          'twisted',
          'pygame'],
      zip_safe=False)
