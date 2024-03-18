<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:template match="seman">
  <html>
    <style>
      table, tr, td {
      text-align: center;
      vertical-align: top;
      }
    </style>
    <body>
      <table>
	<xsl:apply-templates select="node"/>
      </table>
    </body>
  </html>
</xsl:template>

<xsl:template match="node">
  <td>
    <table width="100%">
      <tr bgcolor="DDDDDD">
	<td colspan="1000">
	  <nobr>
	    <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
	    (<xsl:value-of select="@id"/>)
	    <font style="font-family:arial black">
	      <xsl:value-of select="@label"/>
	    </font>
	    <xsl:if test="@name!=''">
	      <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
	      <font style="font-family:helvetica">
		<xsl:value-of select="@name"/>
	      </font>
	    </xsl:if>
	    <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
	  </nobr>
	  <br/>
	  <xsl:if test="location">
	    <nobr>
	      <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
	      <xsl:apply-templates select="location"/>
	      <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
	    </nobr>
	  </xsl:if>
          <table width="100%">
            <xsl:apply-templates select="definedat"/>
            <xsl:apply-templates select="addr"/>
            <xsl:apply-templates select="value"/>
            <xsl:apply-templates select="semtype"/>
	  </table>
	</td>
      </tr>
      <tr>
	<xsl:apply-templates select="node"/>
      </tr>
    </table>
  </td>
</xsl:template>

<xsl:template match="definedat">
  <tr bgcolor="FFCF00">
    <td>
      <nobr>
	<xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
	(<xsl:value-of select="@idx"/>)
	[<xsl:value-of select="@location"/>]
	<xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
      </nobr>
    </td>
  </tr>	
</xsl:template>

<xsl:template match="addr">
  <tr bgcolor="FFCF00">
    <td>
      <nobr>
	<xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
	addr
	<xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
      </nobr>
    </td>
  </tr>	
</xsl:template>

<xsl:template match="value">
  <tr bgcolor="FFCF00">
    <td>
      <nobr>
	<xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
	value=<xsl:value-of select="@value"/>
	<xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
      </nobr>
    </td>
  </tr>	
</xsl:template>

<xsl:template match="semtype">
  <td>
    <table width="100%">
      <tr>
	<td bgcolor="EEBE00" colspan="1000">
	  <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
	  <xsl:value-of select="@type"/>
	  <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
	</td>
      </tr>
      <tr>
	<xsl:apply-templates select="semtypes"/>
	<xsl:apply-templates select="semtype"/>
      </tr>
    </table>
  </td>
</xsl:template>

<xsl:template match="location">
  <nobr>
    <font style="font-family:helvetica">
      <xsl:value-of select="@loc"/>
    </font>
  </nobr>
</xsl:template>

</xsl:stylesheet>
