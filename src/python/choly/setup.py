import setuptools

with open('README.md', 'r') as fh:
    long_description = fh.read()

setuptools.setup(
    name='choly',
    version='0.0.2',
    author='Fierycod',
    packages=setuptools.find_packages(),
    author_email='karol.wojcik@tuta.io',
    description='Holy Lambda Command Line Application',
    install_requires=[
        'click',
        'cfn_flip'
    ],
    long_description=long_description,
    long_description_content_type='text/markdown',
    url='https://github.com/FieryCod/holy-lambda',
    entry_points={
        'console_scripts': [
            'choly= choly.__main__:main'
        ]
    },
    classifiers=[
        'Environment :: Console',
        'License :: OSI Approved :: MIT License',
        'Programming Language :: Python :: 3.7'
    ]
)
