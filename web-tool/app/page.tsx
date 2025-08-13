'use client'

import { useState } from 'react'
import { BankParserFactory } from '@/lib/parsers'
import { CategoryMapper } from '@/lib/categorization'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Separator } from '@/components/ui/separator'
import { SubmissionDialog } from '@/components/submission-dialog'
import { BankTemplates } from '@/components/bank-templates'
import Link from 'next/link'
import { BookOpen } from 'lucide-react'

export default function Home() {
  const [smsBody, setSmsBody] = useState('')
  const [sender, setSender] = useState('')
  const [parseResult, setParseResult] = useState<any>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [showSubmissionDialog, setShowSubmissionDialog] = useState(false)
  const [submissionMode, setSubmissionMode] = useState<'submit' | 'improve'>('submit')

  const handleParse = () => {
    setIsLoading(true)
    
    try {
      const result = BankParserFactory.parse(smsBody, sender, Date.now())
      setParseResult(result)
    } catch (error) {
      setParseResult({
        success: false,
        error: 'An error occurred while parsing the SMS'
      })
    }
    
    setIsLoading(false)
  }

  const handleClear = () => {
    setSmsBody('')
    setSender('')
    setParseResult(null)
  }

  const handleOpenSubmission = (mode: 'submit' | 'improve') => {
    setSubmissionMode(mode)
    setShowSubmissionDialog(true)
  }

  const supportedBanks = BankParserFactory.getSupportedBanks()

  return (
    <div className="container mx-auto py-8 px-4 max-w-4xl">
      <div className="space-y-6">
        <div className="flex justify-between items-start">
          <div>
            <h1 className="text-3xl font-bold">PennyWise SMS Parser</h1>
            <p className="text-muted-foreground mt-2">
              Test if your bank SMS messages are compatible with PennyWise
            </p>
          </div>
          <Link href="/bank-support">
            <Button variant="outline" className="gap-2">
              <BookOpen className="h-4 w-4" />
              Bank Support Docs
            </Button>
          </Link>
        </div>

        <Card>
          <CardHeader>
            <CardTitle>Test Your SMS</CardTitle>
            <CardDescription>
              Paste your bank SMS and sender ID to check if it can be parsed
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="sender">Sender ID</Label>
              <Input
                id="sender"
                placeholder="e.g., HDFCBK, SBIBNK"
                value={sender}
                onChange={(e) => setSender(e.target.value)}
              />
              <p className="text-xs text-muted-foreground">
                The sender ID shown in your SMS app (usually 6 characters)
              </p>
            </div>

            <div className="space-y-2">
              <Label htmlFor="sms">SMS Message</Label>
              <Textarea
                id="sms"
                placeholder="Paste your bank transaction SMS here..."
                value={smsBody}
                onChange={(e) => setSmsBody(e.target.value)}
                rows={6}
              />
            </div>

            <div className="flex gap-2">
              <Button 
                onClick={handleParse} 
                disabled={!smsBody || !sender || isLoading}
              >
                Parse SMS
              </Button>
              <Button 
                variant="outline" 
                onClick={handleClear}
              >
                Clear
              </Button>
            </div>
          </CardContent>
        </Card>

        {parseResult && (
          <Card>
            <CardHeader>
              <CardTitle>
                {parseResult.success ? 'Parsed Successfully' : 'Parsing Failed'}
              </CardTitle>
            </CardHeader>
            <CardContent>
              {parseResult.success ? (
                <div className="space-y-4">
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <p className="text-sm font-medium">Amount</p>
                      <p className="text-2xl font-bold">
                        ₹{parseResult.data.amount?.toLocaleString('en-IN')}
                      </p>
                    </div>
                    <div>
                      <p className="text-sm font-medium">Type</p>
                      <Badge variant={parseResult.data.type === 'EXPENSE' ? 'destructive' : 'default'}>
                        {parseResult.data.type}
                      </Badge>
                    </div>
                  </div>

                  <Separator />

                  <div className="space-y-2">
                    {parseResult.data.merchant && (
                      <div className="flex justify-between">
                        <span className="text-sm font-medium">Merchant</span>
                        <span className="text-sm">{parseResult.data.merchant}</span>
                      </div>
                    )}
                    
                    {parseResult.data.category && (
                      <div className="flex justify-between">
                        <span className="text-sm font-medium">Category</span>
                        <Badge variant="outline">
                          {parseResult.data.category}
                        </Badge>
                      </div>
                    )}

                    {parseResult.data.bankName && (
                      <div className="flex justify-between">
                        <span className="text-sm font-medium">Bank</span>
                        <span className="text-sm">{parseResult.data.bankName}</span>
                      </div>
                    )}

                    {parseResult.data.accountLast4 && (
                      <div className="flex justify-between">
                        <span className="text-sm font-medium">Account</span>
                        <span className="text-sm">****{parseResult.data.accountLast4}</span>
                      </div>
                    )}

                    {parseResult.data.balance !== null && (
                      <div className="flex justify-between">
                        <span className="text-sm font-medium">Balance</span>
                        <span className="text-sm">₹{parseResult.data.balance.toLocaleString('en-IN')}</span>
                      </div>
                    )}

                    {parseResult.data.reference && (
                      <div className="flex justify-between">
                        <span className="text-sm font-medium">Reference</span>
                        <span className="text-sm font-mono text-xs">{parseResult.data.reference}</span>
                      </div>
                    )}
                  </div>

                  {parseResult.confidence && (
                    <>
                      <Separator />
                      <div className="flex justify-between">
                        <span className="text-sm font-medium">Confidence</span>
                        <span className="text-sm">{(parseResult.confidence * 100).toFixed(0)}%</span>
                      </div>
                    </>
                  )}

                  <Separator />
                  
                  <Button 
                    onClick={() => handleOpenSubmission('improve')}
                    variant="outline"
                    className="w-full"
                  >
                    Improve Results
                  </Button>
                </div>
              ) : (
                <div className="space-y-4">
                  <Alert>
                    <AlertDescription>
                      {parseResult.error || 'Unable to parse this SMS. The bank or format may not be supported yet.'}
                    </AlertDescription>
                  </Alert>
                  <Button 
                    onClick={() => handleOpenSubmission('submit')}
                    className="w-full"
                    variant="outline"
                  >
                    Submit Correct Details
                  </Button>
                </div>
              )}
            </CardContent>
          </Card>
        )}

        <Card>
          <CardHeader>
            <CardTitle>Supported Banks</CardTitle>
            <CardDescription>
              Currently supporting {supportedBanks.length} banks
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="flex flex-wrap gap-2">
              {supportedBanks.map((bank) => (
                <Badge key={bank.code} variant="secondary">
                  {bank.name}
                </Badge>
              ))}
            </div>
          </CardContent>
        </Card>

        <BankTemplates />

        <Card>
          <CardHeader>
            <CardTitle>Report Unsupported SMS</CardTitle>
            <CardDescription>
              If your bank SMS isn't parsing correctly, you can submit it for review
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Button 
              variant="outline" 
              className="w-full"
              onClick={() => handleOpenSubmission('submit')}
              disabled={!smsBody || !sender}
            >
              Submit Unsupported SMS
            </Button>
          </CardContent>
        </Card>
      </div>

      <SubmissionDialog
        open={showSubmissionDialog}
        onOpenChange={setShowSubmissionDialog}
        smsBody={smsBody}
        sender={sender}
        parseResult={parseResult}
        mode={submissionMode}
      />
    </div>
  )
}